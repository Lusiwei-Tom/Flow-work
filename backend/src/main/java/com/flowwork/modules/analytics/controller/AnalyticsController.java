package com.flowwork.modules.analytics.controller;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "8-数据分析")
@RestController
@RequestMapping("/api/projects/{projectId}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "项目数据看板（完成率/优先级/逾期/健康度/趋势）")
    @GetMapping("/dashboard")
    @RequirePermission("analytics:dashboard:read")
    public Result<Map<String, Object>> dashboard(@PathVariable Long projectId) {
        return Result.ok(analyticsService.dashboard(projectId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "成员活跃度统计")
    @GetMapping("/members")
    @RequirePermission("analytics:member:read")
    public Result<Map<String, Object>> members(@PathVariable Long projectId) {
        return Result.ok(analyticsService.memberActivity(projectId, SecurityUtils.getCurrentUserId()));
    }
}
