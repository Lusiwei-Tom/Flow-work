package com.flowwork.modules.project.controller;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.project.dto.TaskMoveRequest;
import com.flowwork.modules.project.dto.TaskRequest;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.entity.TaskComment;
import com.flowwork.modules.project.service.TaskCommentService;
import com.flowwork.modules.project.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "4-任务")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskCommentService commentService;

    @Operation(summary = "项目任务列表（可按关键词/负责人/优先级/状态筛选）")
    @GetMapping("/projects/{projectId}/tasks")
    @RequirePermission("project:task:read")
    public Result<List<Task>> list(@PathVariable Long projectId,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Long assigneeId,
                                   @RequestParam(required = false) Integer priority,
                                   @RequestParam(required = false) Integer status) {
        return Result.ok(taskService.listByProject(projectId, SecurityUtils.getCurrentUserId(),
                keyword, assigneeId, priority, status));
    }

    @Operation(summary = "创建任务")
    @PostMapping("/projects/{projectId}/tasks")
    @RequirePermission("project:task:create")
    public Result<Task> create(@PathVariable Long projectId, @Valid @RequestBody TaskRequest req) {
        return Result.ok(taskService.create(projectId, req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "拖拽任务到目标列")
    @PostMapping("/projects/{projectId}/tasks/move")
    @RequirePermission("project:task:update")
    public Result<Task> move(@PathVariable Long projectId, @Valid @RequestBody TaskMoveRequest req) {
        return Result.ok(taskService.move(req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/tasks/{taskId}")
    @RequirePermission("project:task:read")
    public Result<Task> detail(@PathVariable Long taskId) {
        return Result.ok(taskService.detail(taskId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "更新任务")
    @PutMapping("/tasks/{taskId}")
    @RequirePermission("project:task:update")
    public Result<Task> update(@PathVariable Long taskId, @Valid @RequestBody TaskRequest req) {
        return Result.ok(taskService.update(taskId, req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "修改优先级")
    @PatchMapping("/tasks/{taskId}/priority")
    @RequirePermission("project:task:update")
    public Result<Task> priority(@PathVariable Long taskId, @RequestBody Map<String, Integer> body) {
        return Result.ok(taskService.changePriority(taskId, body.get("priority"),
                SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "分配负责人")
    @PatchMapping("/tasks/{taskId}/assignee")
    @RequirePermission("project:task:update")
    public Result<Task> assignee(@PathVariable Long taskId, @RequestBody Map<String, Long> body) {
        return Result.ok(taskService.assign(taskId, body.get("assigneeId"),
                SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "变更状态")
    @PatchMapping("/tasks/{taskId}/status")
    @RequirePermission("project:task:update")
    public Result<Task> status(@PathVariable Long taskId, @RequestBody Map<String, Integer> body) {
        return Result.ok(taskService.changeStatus(taskId, body.get("status"),
                SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/tasks/{taskId}")
    @RequirePermission("project:task:delete")
    public Result<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }

    // ---------- 评论 ----------

    @Operation(summary = "任务评论列表")
    @GetMapping("/tasks/{taskId}/comments")
    @RequirePermission("project:task:read")
    public Result<List<TaskComment>> comments(@PathVariable Long taskId) {
        return Result.ok(commentService.list(taskId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/tasks/{taskId}/comments")
    @RequirePermission("project:task:update")
    public Result<TaskComment> addComment(@PathVariable Long taskId, @RequestBody Map<String, String> body) {
        return Result.ok(commentService.create(taskId, body.get("content"),
                body.get("mentions"), SecurityUtils.getCurrentUserId()));
    }
}
