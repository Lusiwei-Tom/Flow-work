package com.flowwork.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.auth.service.PermissionService;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.mapper.ProjectMapper;
import com.flowwork.modules.project.mapper.ProjectMemberMapper;
import com.flowwork.modules.project.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 个人聚合视图（登录即可，无需项目权限）
 */
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MeController {

    private final TaskService taskService;
    private final PermissionService permissionService;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;

    /** 跨项目聚合：分配给我的任务（进行中 > 待办 > 近 7 天已完成） */
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> myTasks() {
        return Result.ok(taskService.myTasks(SecurityUtils.getCurrentUserId()));
    }

    /** 我的角色档案：系统角色 + 权限码 + 各项目角色 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<String> roles = permissionService.getRoles(userId);
        List<String> perms = permissionService.getPermissions(userId);

        // 我参与的项目 + 在各项目中的角色
        List<Long> projectIds = memberMapper.selectProjectIds(userId);
        List<Map<String, Object>> projects = new ArrayList<>();
        if (!projectIds.isEmpty()) {
            List<Project> list = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                    .in(Project::getId, projectIds)
                    .ne(Project::getStatus, 3)
                    .orderByDesc(Project::getUpdatedAt));
            for (Project p : list) {
                String role = p.getOwnerId().equals(userId) ? "MANAGER" :
                        memberMapper.selectRole(p.getId(), userId);
                if (role == null) role = "OBSERVER";
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                m.put("ownerId", p.getOwnerId());
                m.put("isOwner", p.getOwnerId().equals(userId));
                m.put("projectRole", role);
                projects.add(m);
            }
        }

        // 权限按模块分组
        Map<String, List<String>> groupedPerms = new LinkedHashMap<>();
        for (String p : perms) {
            String module = p.contains(":") ? p.substring(0, p.indexOf(':')) : "other";
            groupedPerms.computeIfAbsent(module, k -> new ArrayList<>()).add(p);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("roles", roles);
        result.put("permissions", perms);
        result.put("groupedPermissions", groupedPerms);
        result.put("projects", projects);
        result.put("projectCount", projects.size());
        return Result.ok(result);
    }
}
