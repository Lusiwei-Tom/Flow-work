package com.flowwork.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 任务拆解结构化输出（ChatClient .entity() 反序列化目标）
 * AI 按需求文档拆解出 2-5 个阶段，每阶段 2-6 个任务；
 * 由 AiService 转换为 TaskRequest 批量落库（标题会加【阶段名】前缀）
 */
@Data
public class TaskBreakdownResponse {

    /** 拆解出的项目阶段列表（必填） */
    @JsonProperty(required = true)
    private List<Phase> phases;

    /**
     * 项目阶段：如"需求分析"、"开发"、"测试上线"
     */
    @Data
    public static class Phase {
        /** 阶段名称 */
        private String name;
        /** 阶段排序号，用于控制创建顺序 */
        private Integer sortOrder;
        /** 该阶段包含的任务列表 */
        private List<TaskItem> tasks;
    }

    /**
     * 具体任务项：转换为看板任务（默认落入待办列）
     */
    @Data
    public static class TaskItem {
        /** 任务标题，要求简洁（不超过 30 字） */
        private String title;
        /** 任务要点描述 */
        private String description;
        /** 优先级：1低 2中 3高 4紧急（缺省时按 2 处理） */
        private Integer priority;
        /** 预估工时（小时） */
        private BigDecimal estimatedHours;
        /** 依赖的任务标题（同批次内，无依赖则为空） */
        private List<String> dependsOn;
    }
}
