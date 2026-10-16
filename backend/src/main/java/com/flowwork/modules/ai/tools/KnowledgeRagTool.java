package com.flowwork.modules.ai.tools;

import com.flowwork.common.annotation.AiTool;
import com.flowwork.modules.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * AI 知识库 RAG 检索工具，供 AI Agent 自动调用
 * <p>
 * 项目 ID 通过 Spring AI ToolContext 由调用方（AiService）随调用链注入，
 * 替代原 ThreadLocal 方案（工具在框架线程池执行时 ThreadLocal 会丢值导致查空）。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeRagTool {

    private final KnowledgeService knowledgeService;

    /** 从 ToolContext 取出当前项目 ID（AiService 通过 .toolContext() 传入） */
    private static Long projectId(ToolContext tc) {
        return (Long) tc.getContext().get("projectId");
    }

    @Tool(description = "当用户询问项目技术方案、接口设计、历史文档等知识库相关问题时，用自然语言问题检索项目知识库，返回相关文档片段")
    @AiTool("searchProjectKnowledge")
    public String searchProjectKnowledge(@ToolParam(description = "检索问题，用自然语言描述") String question,
                                         ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        String context = knowledgeService.retrieveContext(projectId, question);
        return context.isBlank() ? "知识库中没有找到相关内容" : context;
    }
}
