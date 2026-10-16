package com.flowwork.modules.project.controller;

import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.project.dto.MemberRequest;
import com.flowwork.modules.project.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目成员管理：项目内 MANAGER 可操作，成员可查看列表
 */
@RestController
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return Result.ok(memberService.listMembers(projectId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public Result<Map<String, Object>> add(@PathVariable Long projectId, @RequestBody MemberRequest req) {
        return Result.ok(memberService.addMember(projectId, req.getUsername(),
                req.getProjectRole(), SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{userId}")
    public Result<Void> updateRole(@PathVariable Long projectId, @PathVariable Long userId,
                                   @RequestBody MemberRequest req) {
        memberService.updateRole(projectId, userId, req.getProjectRole(), SecurityUtils.getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long projectId, @PathVariable Long userId) {
        memberService.removeMember(projectId, userId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
