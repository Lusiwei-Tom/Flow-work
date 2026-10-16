package com.flowwork.modules.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * AI 项目风险评估报告（接口 POST /api/projects/{projectId}/ai/risk/analyze 的响应体）
 * 由规则引擎（RiskRuleService）扫描项目任务后聚合生成
 */
@Data
@Builder
public class RiskReport {
    /** 整体风险等级：HIGH / MEDIUM / LOW */
    private String level;
    /** 风险评分：按各风险项权重累加，分数越高风险越大 */
    private Integer score;
    /** 命中的风险项明细列表 */
    private List<RiskItem> items;
    /** AI 生成的风险改进建议 */
    private String aiSuggestion;

    /**
     * 单条风险项：对应一条命中规则（如逾期任务、成员过载、高优先级无负责人等）
     */
    @Data
    @Builder
    public static class RiskItem {
        /** 命中的规则标识，如 OVERDUE_TASK、DUE_SOON、MEMBER_OVERLOADED 等 */
        private String rule;
        /** 该风险项的等级：HIGH / MEDIUM / LOW */
        private String level;
        /** 风险描述：说明具体触发原因及关联任务 */
        private String detail;
    }
}
