package com.flowwork.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求实体
 */
@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    private String password;
    /** 设备标识，用于 refresh token 滑动续期 */
    private String deviceId;
}
