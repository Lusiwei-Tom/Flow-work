package com.flowwork.modules.analytics.service;

import com.flowwork.modules.ai.service.ProjectDataGatherService;
import com.flowwork.modules.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据分析服务：看板汇总、项目健康度、成员活跃度
 * 统计 SQL 复用 TaskMapper，避免重复实现
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProjectAccessService accessService;
    private final ProjectDataGatherService dataGatherService;

    public Map<String, Object> dashboard(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);// 校验用户是否是项目成员
        Map<String, Object> result = new HashMap<>(dataGatherService.gather(projectId));

        // 健康度评分：完成率得分 + 逾期扣分
        //算法设计：健康度 = 完成率得分（满分60） + 基础分（40） - 逾期扣分（每个逾期扣8分）
        long total = ((Number) result.getOrDefault("totalTasks", 0)).longValue();
        long finished = ((Number) result.getOrDefault("finished", 0)).longValue();
        long overdue = ((Number) result.getOrDefault("overdueCount", 0)).longValue();
        int health = total == 0 ? 100
                : (int) Math.max(0, Math.round(60.0 * finished / total) + 40 - overdue * 8);
        result.put("healthScore", health);
        // 健康度等级：根据健康度评分判断，用一个三元运算符做分级：>= 80 良好，>= 60 一般，< 60 风险。
        result.put("healthLevel", health >= 80 ? "良好" : health >= 60 ? "一般" : "风险");
        return result;
    }

    public Map<String, Object> memberActivity(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        return Map.of("members", dataGatherService.gather(projectId).get("memberStats"));
    }
}
