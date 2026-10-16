package com.flowwork.modules.auth.controller;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.modules.auth.entity.InviteCode;
import com.flowwork.modules.auth.service.InviteCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "邀请码管理")
@RestController
@RequestMapping("/api/invite-codes")
@RequiredArgsConstructor
public class InviteCodeController {

    private final InviteCodeService inviteCodeService;

    @Operation(summary = "生成邀请码")
    @PostMapping
    @RequirePermission("auth:user:manage")
    public Result<InviteCode> create(@RequestBody InviteCode req) {
        return Result.ok(inviteCodeService.create(req));
    }

    @Operation(summary = "邀请码列表")
    @GetMapping
    @RequirePermission("auth:user:manage")
    public Result<List<InviteCode>> list() {
        return Result.ok(inviteCodeService.list());
    }

    @Operation(summary = "停用邀请码")
    @PutMapping("/{id}/disable")
    @RequirePermission("auth:user:manage")
    public Result<Void> disable(@PathVariable Long id) {
        inviteCodeService.disable(id);
        return Result.ok();
    }

    @Operation(summary = "删除邀请码")
    @DeleteMapping("/{id}")
    @RequirePermission("auth:user:manage")
    public Result<Void> delete(@PathVariable Long id) {
        inviteCodeService.delete(id);
        return Result.ok();
    }
}
