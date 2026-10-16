package com.flowwork.modules.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.ai.dto.RiskReport;
import com.flowwork.modules.ai.entity.ProjectReport;
import com.flowwork.modules.ai.mapper.ProjectReportMapper;
import com.flowwork.modules.ai.service.AiService;
import com.flowwork.modules.project.entity.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI 能力 Controller：统一暴露项目管家对话、任务拆解、会议解析、风险分析、周报、知识库问答等接口
 * 所有接口路径前缀：/api/projects/{projectId}/ai
 * 权限由 @RequirePermission 自定义注解 + AOP 切面校验，用户身份由 SecurityUtils 从 JWT 中提取
 */
@Tag(name = "7-AI 能力") // Swagger 文档分组标签，在 /swagger-ui.html 中按此名归类展示
@RestController // 标记为 RESTful 控制器：返回值默认序列化为 JSON，被 Spring 扫描注册为 Bean
@RequestMapping("/api/projects/{projectId}/ai") // 类级基础路径，所有方法路径继承此前缀；{projectId} 为路径变量
@RequiredArgsConstructor // Lombok：为所有 final 字段生成构造方法，Spring 通过构造器完成依赖注入（替代 @Autowired）
public class AiController {

    private final AiService aiService;
    private final ProjectReportMapper reportMapper;

    /**
     * AI 项目管家对话（ToolCalling）：同步返回完整回复
     * @Operation   Swagger 接口摘要说明
     * @PostMapping  映射 POST 请求至该方法
     * @RequirePermission 自定义权限注解，需拥有 ai:agent:execute 权限
     * @PathVariable 从 URL 路径中提取 {projectId}
     * @RequestBody  将请求体 JSON 反序列化为 Map，取 message 字段作为用户输入
     */
    @Operation(summary = "AI 项目管家对话（ToolCalling）")
    @PostMapping("/chat")
    @RequirePermission("ai:agent:execute")
    public Result<Map<String, String>> chat(@PathVariable Long projectId,
                                            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String answer = aiService.chat(projectId, body.get("message"), userId);
        return Result.ok(Map.of("answer", answer));
    }

    /**
     * AI 对话历史查询（按时间正序）
     * @RequestParam 读取 query 参数 limit，默认 50 条，最多 200 条
     */
    @Operation(summary = "AI 对话历史（最近 N 条，按时间正序）")
    @GetMapping("/chat/history")
    @RequirePermission("ai:agent:execute")
    public Result<List<Map<String, Object>>> history(@PathVariable Long projectId,
                                                     @RequestParam(defaultValue = "50") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> list = aiService.chatHistory(projectId, userId, limit).stream()
                .map(m -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    map.put("createdAt", m.getCreatedAt());
                    return map;
                }).toList();// stream() 方法将 Flux 转换为 Stream，map() 方法将每个元素转换为 Map，包含 id、role、content、createdAt 字段
        return Result.ok(list);
    }

    /**
     * 清空当前用户在该项目的 AI 对话历史
     * @DeleteMapping 映射 DELETE 请求
     */
    @Operation(summary = "清空当前用户在该项目的 AI 对话历史")
    @DeleteMapping("/chat/history")
    @RequirePermission("ai:agent:execute")// 校验用户是否是项目成员
    public Result<Void> clearHistory(@PathVariable Long projectId) {
        aiService.clearHistory(projectId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }

    /**
     * AI 项目管家流式对话（SSE）
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE 声明响应类型为 text/event-stream
     * Flux<ServerSentEvent<String>> 是 Reactor 响应式流：每个 chunk 包装为 SSE 事件推送给前端
     * 前端通过 EventSource 监听该流式接口，实现打字机效果
     */
    @Operation(summary = "AI 项目管家流式对话（SSE）")// 显示在 Swagger 文档中
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission("ai:agent:execute")// 校验用户是否是项目成员
    public Flux<ServerSentEvent<String>> chatStream(@PathVariable Long projectId,
                                                    @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return aiService.chatStream(projectId, body.get("message"), userId)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    /**
     * AI 任务拆解：根据需求文档结构化输出任务并自动入库
     */
    @Operation(summary = "AI 任务拆解（一句话生成结构化任务并入库）")
    @PostMapping("/tasks/breakdown")
    @RequirePermission("ai:task:generate")// 校验用户是否是项目成员
    public Result<List<Task>> breakDown(@PathVariable Long projectId,
                                        @RequestBody Map<String, String> body) {
        return Result.ok(aiService.breakDownTasks(projectId, body.get("requirement"),
                SecurityUtils.getCurrentUserId()));
    }

    /**
     * AI 会议纪要解析：从会议记录提取行动项生成任务
     */
    @Operation(summary = "AI 会议纪要生成任务")
    @PostMapping("/meeting/parse")
    @RequirePermission("ai:meeting:parse")// 校验用户是否是项目成员
    public Result<List<Task>> parseMeeting(@PathVariable Long projectId,
                                           @RequestBody Map<String, String> body) {
        return Result.ok(aiService.parseMeeting(projectId, body.get("meetingText"),
                SecurityUtils.getCurrentUserId()));
    }

    /**
     * AI 风险分析：规则引擎扫描风险项 + AI 生成应对建议
     */
    @Operation(summary = "AI 风险分析（规则引擎 + AI 建议）")
    @PostMapping("/risk/analyze")
    @RequirePermission("ai:risk:analyze")
    public Result<RiskReport> analyzeRisk(@PathVariable Long projectId) {
        return Result.ok(aiService.analyzeRisk(projectId, SecurityUtils.getCurrentUserId()));
    }

    /**
     * AI 自动生成周报：聚合项目数据 → AI 生成 Markdown 周报 → 落库
     */
    @Operation(summary = "AI 自动生成周报")
    @PostMapping("/reports/weekly")
    @RequirePermission("ai:report:generate")
    public Result<ProjectReport> weeklyReport(@PathVariable Long projectId) {
        return Result.ok(aiService.weeklyReport(projectId, SecurityUtils.getCurrentUserId()));
    }

    /**
     * 报告列表查询：按创建时间倒序返回该项目所有报告
     */
    @Operation(summary = "报告列表")
    @GetMapping("/reports")
    @RequirePermission("ai:report:generate")
    public Result<List<ProjectReport>> reports(@PathVariable Long projectId) {
        return Result.ok(reportMapper.selectList(new LambdaQueryWrapper<ProjectReport>()
                .eq(ProjectReport::getProjectId, projectId)
                .orderByDesc(ProjectReport::getCreatedAt)));
    }

    /**
     * 知识库 RAG 问答（同步）：检索相关文档片段 → AI 据此回答
     */
    @Operation(summary = "知识库 RAG 问答")
    @PostMapping("/knowledge/qa")
    @RequirePermission("knowledge:rag:query")
    public Result<Map<String, String>> knowledgeQa(@PathVariable Long projectId,
                                                   @RequestBody Map<String, String> body) {
        String answer = aiService.knowledgeQa(projectId, body.get("question"),
                SecurityUtils.getCurrentUserId());
        return Result.ok(Map.of("answer", answer));
    }

    /**
     * 知识库 RAG 问答（流式 SSE）：实时推送 AI 生成的 token 流
     */
    @Operation(summary = "知识库 RAG 问答（流式 SSE）")
    @PostMapping(value = "/knowledge/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission("knowledge:rag:query")
    public Flux<ServerSentEvent<String>> knowledgeQaStream(@PathVariable Long projectId,
                                                           @RequestBody Map<String, String> body) {
        return aiService.knowledgeQaStream(projectId, body.get("question"),
                        SecurityUtils.getCurrentUserId())
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
