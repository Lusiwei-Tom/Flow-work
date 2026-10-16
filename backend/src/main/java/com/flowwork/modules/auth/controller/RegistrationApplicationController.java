package com.flowwork.modules.auth.controller;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.modules.auth.entity.RegistrationApplication;
import com.flowwork.modules.auth.service.RegistrationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "注册申请")
@RestController
@RequestMapping("/api/registration-applications")
@RequiredArgsConstructor
public class RegistrationApplicationController {

    private final RegistrationApplicationService applicationService;

    @Operation(summary = "提交注册申请（公开）")
    @PostMapping
    public Result<RegistrationApplication> submit(@RequestBody RegistrationApplication req) {
        return Result.ok(applicationService.submit(req));
    }

    @Operation(summary = "申请列表")
    @GetMapping
    @RequirePermission("auth:user:manage")
    public Result<List<RegistrationApplication>> list(@RequestParam(required = false) String status) {
        return Result.ok(applicationService.list(status));
    }

    @Operation(summary = "审批通过")
    @PutMapping("/{id}/approve")
    @RequirePermission("auth:user:manage")
    public Result<RegistrationApplication> approve(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("reviewNote") : null;
        return Result.ok(applicationService.approve(id, note));
    }

    @Operation(summary = "审批拒绝")
    @PutMapping("/{id}/reject")
    @RequirePermission("auth:user:manage")
    public Result<RegistrationApplication> reject(@PathVariable Long id,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("reviewNote") : null;
        return Result.ok(applicationService.reject(id, note));
    }
}
