package com.flowwork.modules.auth.controller;

import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.auth.dto.*;
import com.flowwork.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "1-认证授权")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return Result.ok(authService.login(req, clientIp(http)));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    @Operation(summary = "发送短信验证码(演示)")
    @PostMapping("/sms/code")
    public Result<Map<String, String>> sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        return Result.ok(Map.of("code", authService.sendSmsCode(phone)));
    }

    @Operation(summary = "手机号验证码登录")
    @PostMapping("/login/phone")
    public Result<LoginResponse> phoneLogin(@RequestBody Map<String, String> body) {
        return Result.ok(authService.phoneLogin(body.get("phone"), body.get("code"), body.get("deviceId")));
    }

    @Operation(summary = "刷新 Token（滑动续期 + 旋转）")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return Result.ok(authService.refresh(req.getRefreshToken(), req.getDeviceId()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody Map<String, String> body) {
        authService.logout(body.getOrDefault("refreshToken", ""));
        return Result.ok();
    }

    @Operation(summary = "获取当前登录用户（含权限码）")
    @GetMapping("/me")
    public Result<LoginResponse.UserInfoVO> me() {
        return Result.ok(authService.currentUser(SecurityUtils.getCurrentUserId()));
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
