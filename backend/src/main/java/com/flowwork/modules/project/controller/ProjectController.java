package com.flowwork.modules.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.project.dto.ProjectRequest;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "2-项目管理")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "我的项目列表（分页）")
    @GetMapping
    @RequirePermission("project:project:read")
    public Result<IPage<Project>> mine(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "12") int size) {
        return Result.ok(projectService.listMine(SecurityUtils.getCurrentUserId(), page, size));
    }

    @Operation(summary = "创建项目")
    @PostMapping
    @RequirePermission("project:project:create")
    public Result<Project> create(@Valid @RequestBody ProjectRequest req) {
        return Result.ok(projectService.create(req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    @RequirePermission("project:project:read")
    public Result<Project> detail(@PathVariable Long id) {
        return Result.ok(projectService.detail(id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    @RequirePermission("project:project:update")
    public Result<Project> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        return Result.ok(projectService.update(id, req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "归档项目")
    @PostMapping("/{id}/archive")
    @RequirePermission("project:project:update")
    public Result<Project> archive(@PathVariable Long id) {
        return Result.ok(projectService.archive(id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    @RequirePermission("project:project:delete")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
