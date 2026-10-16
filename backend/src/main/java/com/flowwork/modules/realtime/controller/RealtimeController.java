package com.flowwork.modules.realtime.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.project.service.ProjectAccessService;
import com.flowwork.modules.realtime.entity.Activity;
import com.flowwork.modules.realtime.event.RealtimeEvent;
import com.flowwork.modules.realtime.mapper.ActivityMapper;
import com.flowwork.modules.realtime.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "5-实时动态")
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class RealtimeController {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;
    private final ProjectAccessService accessService;

    @Operation(summary = "项目动态流（最近 50 条）")
    @GetMapping("/activities")
    public Result<List<Activity>> activities(@PathVariable Long projectId) {
        accessService.requireMember(projectId, SecurityUtils.getCurrentUserId());
        return Result.ok(activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getProjectId, projectId)
                .orderByDesc(Activity::getCreatedAt)
                .last("LIMIT 50")));
    }

    @Operation(summary = "在线成员")
    @GetMapping("/online")
    public Result<Set<String>> online(@PathVariable Long projectId) {
        accessService.requireMember(projectId, SecurityUtils.getCurrentUserId());
        return Result.ok(activityService.onlineMembers(projectId));
    }

    @Operation(summary = "心跳：上报在线状态")
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@PathVariable Long projectId) {
        activityService.heartbeat(projectId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }

    @Operation(summary = "离开项目")
    @PostMapping("/leave")
    public Result<Void> leave(@PathVariable Long projectId) {
        activityService.leave(projectId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
