package com.flowwork.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.ai.dto.MeetingParseResponse;
import com.flowwork.modules.ai.dto.RiskReport;
import com.flowwork.modules.ai.dto.TaskBreakdownResponse;
import com.flowwork.modules.ai.entity.ChatMessage;
import com.flowwork.modules.ai.entity.ProjectReport;
import com.flowwork.modules.ai.mapper.ChatMessageMapper;
import com.flowwork.modules.ai.mapper.ProjectReportMapper;
import com.flowwork.modules.ai.tools.KnowledgeRagTool;
import com.flowwork.modules.ai.tools.ProjectQueryTool;
import com.flowwork.modules.ai.tools.TaskOperationTool;
import com.flowwork.modules.auth.entity.SysUser;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import com.flowwork.modules.knowledge.service.KnowledgeService;
import com.flowwork.modules.project.entity.BoardColumn;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.dto.TaskRequest;
import com.flowwork.modules.project.mapper.BoardColumnMapper;
import com.flowwork.modules.project.mapper.ProjectMemberMapper;
import com.flowwork.modules.project.service.ProjectAccessService;
import com.flowwork.modules.project.service.TaskService;
import com.flowwork.modules.realtime.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 核心服务：
 * - ChatClient + @Tool 工具调用（项目管家，支持流式 SSE）
 * - ChatClient 结构化输出（任务拆解 / 会议解析）
 * - 规则引擎 + AI 混合（风险分析）
 * - RAG 知识库问答 / 数据聚合周报
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    /** ChatClient 构建器，用于构造同步/流式 AI 调用客户端 */
    private final ChatClient.Builder chatClientBuilder;
    /** 工具：查询项目任务/逾期任务/搜索任务 */
    private final ProjectQueryTool projectQueryTool;
    /** 工具：修改任务优先级、负责人、状态等写操作 */
    private final TaskOperationTool taskOperationTool;
    /** 工具：检索项目知识库（RAG） */
    private final KnowledgeRagTool knowledgeRagTool;
    private final KnowledgeService knowledgeService;
    private final RiskRuleService riskRuleService;
    private final ProjectDataGatherService dataGatherService;
    private final ProjectAccessService accessService;
    private final TaskService taskService;
    private final BoardColumnMapper columnMapper;
    private final SysUserMapper userMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectReportMapper reportMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ActivityService activityService;
    /** Redis 限流器：控制单用户 AI 调用频率 */
    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    /** AI 开关：DeepSeek API Key，未配置则禁用所有 AI 功能 */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /** AI 管家系统提示词：定义工具调用范围与写操作确认规则 */
    private static final String AGENT_SYSTEM_PROMPT = """
            你是 FlowWork 项目协作平台的 AI 项目管家。你可以：
            1. 调用工具查询当前项目的任务、逾期任务、搜索任务；
            2. 调用工具修改任务优先级、负责人、状态（修改类操作必须先向用户确认意图后再执行，若用户已明确要求则直接执行）；
            3. 调用工具检索项目知识库回答技术问题。
            要求：回答简洁专业；涉及数据时优先使用工具返回的真实数据，不要编造；
            执行写操作后告知用户操作结果。
            """;

    /**
     * AI 功能可用性校验：API Key 已配置且未触发限流（10 次/60 秒/用户）
     */
    private void ensureAiEnabled(Long userId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ResultCode.AI_DISABLED, "未配置 DEEPSEEK_API_KEY，AI 功能暂不可用");
        }
        if (!rateLimiter.isAllowed(userId, 10, 60)) {
            throw new BusinessException(ResultCode.RATE_LIMITED);
        }
    }

    // ---------- 1. AI 项目管家对话（ToolCalling） ----------

    /**
     * AI 项目管家对话（ToolCalling）：注入 3 个 @Tool 工具，AI 可查询/修改项目数据
     * 同步返回完整回复，对话记录落库
     */
    public String chat(Long projectId, String message, Long userId) {
        ensureAiEnabled(userId);
        accessService.requireMember(projectId, userId);
        saveChat(projectId, userId, "user", message);
        // 项目/用户身份通过 Spring AI ToolContext 传给 @Tool 方法（随调用链传递，不依赖线程）
        Map<String, Object> toolCtx = Map.of("projectId", projectId, "userId", userId);
        String answer;
        try {
            answer = chatClientBuilder.build().prompt()
                    .system(AGENT_SYSTEM_PROMPT)
                    .user(message)
                    .tools(projectQueryTool, taskOperationTool, knowledgeRagTool)
                    .toolContext(toolCtx)
                    .call()
                    .content();
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("AI Agent 调用失败", e);
            throw new BusinessException(ResultCode.AI_TIMEOUT, "AI 服务异常: " + rootMessage(e));
        }
        saveChat(projectId, userId, "ai", answer);
        return answer;
    }

    /**
     * 流式对话（SSE）：复用同一批 @Tool 工具，走 ChatClient 流式工具调用通道
     */
    public Flux<String> chatStream(Long projectId, String message, Long userId) {
        ensureAiEnabled(userId);// 校验 AI 功能是否可用
        accessService.requireMember(projectId, userId);// 校验用户是否是项目成员
        saveChat(projectId, userId, "user", message);// 保存用户消息到数据库
        // 项目/用户身份通过 Spring AI ToolContext 传给 @Tool 方法（随调用链传递，不依赖线程）
        Map<String, Object> toolCtx = Map.of("projectId", projectId, "userId", userId);
        // 流式对话缓冲区：用于存储 AI 回复的每个 chunk
        AtomicReference<StringBuilder> buf = new AtomicReference<>(new StringBuilder());//
        try {
            return chatClientBuilder.build().prompt()
                    .system(AGENT_SYSTEM_PROMPT)
                    .user(message)
                    .tools(projectQueryTool, taskOperationTool, knowledgeRagTool)
                    .toolContext(toolCtx)// 传递项目/用户身份
                    .stream()
                    .content()
                    .doOnNext(chunk -> buf.get().append(chunk))
                    .doFinally(s -> {
                        String full = buf.get().toString();
                        if (!full.isBlank()) saveChat(projectId, userId, "ai", full);
                    })
                    .onErrorResume(e -> Flux.just("[AI 服务异常] " + rootMessage(e)));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AI_TIMEOUT, "AI 服务异常: " + rootMessage(e));
        }
    }

    /** 对话历史（最近 limit 条，按时间正序返回便于前端渲染） */
    public List<ChatMessage> chatHistory(Long projectId, Long userId, int limit) {
        accessService.requireMember(projectId, userId);// 校验用户是否是项目成员
        List<ChatMessage> list = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getProjectId, projectId)
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
        java.util.Collections.reverse(list);
        return list;
    }

    /** 清空当前用户在该项目的对话历史 */
    public void clearHistory(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getProjectId, projectId)
                .eq(ChatMessage::getUserId, userId));
    }

    /**
     * 持久化单条对话记录（user/ai），失败仅告警不影响主流程
     */
    private void saveChat(Long projectId, Long userId, String role, String content) {
        try {
            chatMessageMapper.insert(ChatMessage.builder()
                    .projectId(projectId)
                    .userId(userId)
                    .role(role)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("保存对话记录失败: {}", e.getMessage());
        }
    }

    // ---------- 2. AI 任务拆解（结构化输出 + 自动入库） ----------

    /**
     * AI 任务拆解：按需求文档结构化输出 2-5 阶段的任务，统一落入待办列并批量入库
     */
    public List<Task> breakDownTasks(Long projectId, String requirement, Long userId) {
        ensureAiEnabled(userId);// 校验 AI 功能可用（API Key 已配置 + 未触发限流）
        accessService.requireWritable(projectId, userId);// 校验用户是项目成员且有写权限
        // 构造提示词：告诉 AI 拆解规则（2-5 阶段、每阶段 2-6 任务、字段格式），
        // %s 占位符替换为用户输入的需求文档，要求只输出 JSON
        String prompt = """
                你是资深项目管理专家。请根据下面的项目需求，拆解为 2-5 个阶段（Phase），
                每个阶段包含 2-6 个具体可执行任务。任务标题简洁（不超过 30 字），
                description 描述任务要点，estimatedHours 给出合理估时（小时），
                priority 取 1-4，dependsOn 填依赖的其他任务标题（没有则空数组）。
                只输出 JSON，不要输出任何解释。

                项目需求：
                %s
                """.formatted(requirement);

        // 调用 AI 并反序列化：.entity() 将 AI 返回的 JSON 自动映射为 TaskBreakdownResponse 对象
        TaskBreakdownResponse parsed;
        try {
            parsed = chatClientBuilder.build().prompt()
                    .user(prompt)
                    .call()
                    .entity(TaskBreakdownResponse.class);
        } catch (Exception e) {
            log.error("任务拆解结构化输出失败", e);
            throw new BusinessException(ResultCode.STRUCTURED_OUTPUT_FAIL, rootMessage(e));
        }
        // 校验结构化结果有效性：AI 未返回任何阶段视为失败
        if (parsed == null || parsed.getPhases() == null || parsed.getPhases().isEmpty()) {
            throw new BusinessException(ResultCode.STRUCTURED_OUTPUT_FAIL, "AI 未返回有效任务结构");
        }

        // 查询项目的第一个启用中的看板列（按 sortOrder 升序取首个，即"待办"列）
        BoardColumn todoColumn = columnMapper.selectList(new LambdaQueryWrapper<BoardColumn>()
                        .eq(BoardColumn::getProjectId, projectId)
                        .eq(BoardColumn::getStatus, 1).orderByAsc(BoardColumn::getSortOrder)
                        .last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "看板列不存在"));

        // 遍历 AI 拆解结果：将每个 TaskItem 转换为 TaskRequest
        List<TaskRequest> requests = new ArrayList<>();
        int phaseIndex = 0;
        for (TaskBreakdownResponse.Phase phase : parsed.getPhases()) {
            if (phase.getTasks() == null) continue;
            for (TaskBreakdownResponse.TaskItem item : phase.getTasks()) {
                TaskRequest req = new TaskRequest();
                req.setColumnId(todoColumn.getId()); // 统一落入待办列
                req.setTitle("【" + phase.getName() + "】" + item.getTitle()); // 标题加【阶段名】前缀
                req.setDescription(item.getDescription());
                req.setPriority(item.getPriority() == null ? 2 : item.getPriority()); // 缺省优先级"中"
                req.setEstimatedHours(item.getEstimatedHours());
                req.setAiGenerated(true); // 标记为 AI 生成
                req.setAiSource("BREAKDOWN"); // 来源：任务拆解（区别于 MEETING 会议解析）
                requests.add(req);
            }
            phaseIndex++;
        }
        // 批量创建任务并记录项目动态："AI 根据需求拆解生成了 N 个任务"
        List<Task> saved = taskService.createAiBatch(projectId, requests, userId, "BREAKDOWN");
        activityService.record(projectId, null, "AI_TASK_BREAKDOWN", userId, null,
                "AI 根据需求拆解生成了 " + saved.size() + " 个任务", true);
        return saved;
    }

    // ---------- 3. 会议纪要解析（结构化输出 + 自动入库） ----------

    /**
     * 会议纪要解析：AI 提取行动项生成任务，按姓名匹配项目成员后落入待办列
     */
    public List<Task> parseMeeting(Long projectId, String meetingText, Long userId) {
        ensureAiEnabled(userId);
        accessService.requireWritable(projectId, userId);
        String prompt = """
                你是项目助理。请分析以下会议记录，提取需要跟进的行动项：
                每个行动项生成一个任务，包含标题、描述、负责人姓名(assigneeName，没有则留空)、
                截止日期(dueDate，格式 yyyy-MM-dd，没有明确日期则留空)、优先级(1-4)。
                summary 用 100 字内总结会议要点。只输出 JSON。

                会议记录：
                %s
                """.formatted(meetingText);

        MeetingParseResponse parsed;
        try {
            parsed = chatClientBuilder.build().prompt()
                    .user(prompt)
                    .call()
                    .entity(MeetingParseResponse.class);
        } catch (Exception e) {
            log.error("会议解析结构化输出失败", e);
            throw new BusinessException(ResultCode.STRUCTURED_OUTPUT_FAIL, rootMessage(e));
        }
        if (parsed == null || parsed.getTasks() == null) {
            throw new BusinessException(ResultCode.STRUCTURED_OUTPUT_FAIL, "AI 未返回有效任务");
        }

        BoardColumn todoColumn = columnMapper.selectList(new LambdaQueryWrapper<BoardColumn>()
                        .eq(BoardColumn::getProjectId, projectId)
                        .eq(BoardColumn::getStatus, 1).orderByAsc(BoardColumn::getSortOrder)
                        .last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "看板列不存在"));

        List<TaskRequest> requests = new ArrayList<>();
        for (MeetingParseResponse.MeetingTask mt : parsed.getTasks()) {
            TaskRequest req = new TaskRequest();
            req.setColumnId(todoColumn.getId());
            req.setTitle(mt.getTitle());
            req.setDescription(buildMeetingDesc(mt, parsed.getSummary()));
            req.setPriority(mt.getPriority() == null ? 2 : mt.getPriority());
            req.setDueDate(mt.getDueDate());
            req.setAssigneeId(resolveMemberId(projectId, mt.getAssigneeName()));
            req.setAiGenerated(true);
            req.setAiSource("MEETING");
            requests.add(req);
        }
        List<Task> saved = taskService.createAiBatch(projectId, requests, userId, "MEETING");
        activityService.record(projectId, null, "AI_MEETING_PARSE", userId, null,
                "AI 从会议纪要生成了 " + saved.size() + " 个任务", true);
        return saved;
    }

    /** 拼接会议任务描述：正文 + 负责人 */
    private String buildMeetingDesc(MeetingParseResponse.MeetingTask mt, String summary) {
        StringBuilder sb = new StringBuilder();
        if (mt.getDescription() != null) sb.append(mt.getDescription());
        if (mt.getAssigneeName() != null && !mt.getAssigneeName().isBlank()) {
            sb.append("\n负责人：").append(mt.getAssigneeName());
        }
        return sb.toString();
    }

    /** 按昵称查找用户，仅当其为该项目成员时返回 userId，否则返回 null */
    private Long resolveMemberId(Long projectId, String nickname) {
        if (nickname == null || nickname.isBlank()) return null;
        SysUser u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getNickname, nickname).last("LIMIT 1"));
        if (u == null) return null;
        String role = memberMapper.selectRole(projectId, u.getId());
        return role != null ? u.getId() : null;
    }

    // ---------- 4. 风险分析（规则兜底 + AI 解读） ----------

    /**
     * 项目风险分析：规则引擎扫描风险项 → 计算等级与评分 → AI 生成应对建议
     * AI 建议失败时降级返回提示文案，不影响规则结果
     */
    public RiskReport analyzeRisk(Long projectId, Long userId) {
        ensureAiEnabled(userId);
        Project project = accessService.requireProject(projectId);
        accessService.requireMember(projectId, userId);

        List<RiskReport.RiskItem> items = riskRuleService.evaluate(project);
        RiskReport report = RiskReport.builder()
                .level(riskRuleService.levelOf(items))
                .score(riskRuleService.scoreOf(items))
                .items(items)
                .build();

        // AI 综合解读（即使规则为空，AI 也能给出整体评估；AI 失败不影响规则结果返回）
        try {
            Map<String, Object> stats = dataGatherService.gather(projectId);
            String aiPrompt = """
                    你是项目风险顾问。以下是项目规则引擎发现的风险和统计数据，请给出 3-5 条具体、可执行的应对建议（中文，分条列出）：
                    风险项：%s
                    统计数据：%s
                    """.formatted(objectMapper.writeValueAsString(items),
                    objectMapper.writeValueAsString(stats));
            String suggestion = chatClientBuilder.build().prompt().user(aiPrompt).call().content();
            report.setAiSuggestion(suggestion);
        } catch (Exception e) {
            log.warn("AI 风险建议生成失败（规则结果仍返回）: {}", e.getMessage());
            report.setAiSuggestion("（AI 建议暂不可用，请查看规则风险项）");
        }

        activityService.record(projectId, null, "AI_RISK_ANALYSIS", userId, null,
                "AI 完成项目风险分析，风险等级：" + report.getLevel(), true);
        return report;
    }

    // ---------- 5. 自动周报（聚合 + AI + 落库） ----------

    /**
     * 自动周报：聚合项目统计数据 → AI 生成 Markdown 周报 → 落库并记录活动
     */
    public ProjectReport weeklyReport(Long projectId, Long userId) {
        ensureAiEnabled(userId);
        Project project = accessService.requireProject(projectId);// 校验项目是否存在
        accessService.requireMember(projectId, userId);

        Map<String, Object> stats = dataGatherService.gather(projectId);
        String prompt = """
                你是项目经理，请根据以下项目数据生成一份中文周报（Markdown 格式），包含：
                ## 一、本周进展  ## 二、任务统计（完成率/进行中/待办） ## 三、风险与阻塞（逾期/临期）
                ## 四、成员产出  ## 五、下周计划建议。语言简练，用数据说话，不要编造数据中没有的信息。
                项目名称：%s
                统计数据：%s
                """.formatted(project.getName(), writeJson(stats));

        String content;
        try {
            content = chatClientBuilder.build().prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("周报生成失败", e);
            throw new BusinessException(ResultCode.AI_TIMEOUT, rootMessage(e));
        }

        ProjectReport report = new ProjectReport();
        report.setProjectId(projectId);
        report.setType("WEEKLY");
        report.setTitle(project.getName() + " 项目周报 " + LocalDate.now());
        report.setContent(content);
        report.setAiGenerated(1);
        report.setCreatedBy(userId);
        reportMapper.insert(report);

        activityService.record(projectId, null, "AI_WEEKLY_REPORT", userId, null,
                "AI 生成了项目周报", true);
        return report;
    }

    // ---------- 6. 知识库 RAG 问答 ----------

    /**
     * 知识库 RAG 问答（同步）：检索相关文档片段作为上下文，AI 据此回答
     * 无匹配资料时直接返回提示，不调用 AI
     */
    public String knowledgeQa(Long projectId, String question, Long userId) {
        ensureAiEnabled(userId);
        accessService.requireMember(projectId, userId);
        String context = knowledgeService.retrieveContext(projectId, question);
        if (context.isBlank()) {
            return "知识库中没有找到与该问题相关的资料，请先上传项目文档。";
        }
        String prompt = """
                请严格根据下面的参考资料回答用户问题。若资料中没有答案，请说明"根据现有知识库无法回答"，不要编造。

                参考资料：
                %s

                用户问题：%s
                """.formatted(context, question);
        try {
            return chatClientBuilder.build().prompt().user(prompt).call().content();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AI_TIMEOUT, rootMessage(e));
        }
    }

    /**
     * 知识库 RAG 问答（流式）。无工具调用，走 ChatClient.stream() 直接返回 token 流。
     * context 为空或异常时以单元素 Flux 返回提示/错误文本，前端可正常收尾。
     */
    public Flux<String> knowledgeQaStream(Long projectId, String question, Long userId) {
        ensureAiEnabled(userId);
        accessService.requireMember(projectId, userId);
        String context = knowledgeService.retrieveContext(projectId, question);
        if (context.isBlank()) {
            return Flux.just("知识库中没有找到与该问题相关的资料，请先上传项目文档。");
        }
        String prompt = """
                请严格根据下面的参考资料回答用户问题。若资料中没有答案，请说明"根据现有知识库无法回答"，不要编造。

                参考资料：
                %s

                用户问题：%s
                """.formatted(context, question);
        try {
            return chatClientBuilder.build().prompt().user(prompt).stream().content()
                    .onErrorResume(e -> Flux.just("[AI 服务异常] " + rootMessage(e)));
        } catch (Exception e) {
            return Flux.just("[AI 服务异常] " + rootMessage(e));
        }
    }

    /** 序列化对象为 JSON，失败时退回 toString */
    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    /** 提取异常根因消息，截断至 200 字以避免日志过长 */
    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String msg = t.getMessage();
        return msg == null ? e.getClass().getSimpleName() : (msg.length() > 200 ? msg.substring(0, 200) : msg);
    }
}
