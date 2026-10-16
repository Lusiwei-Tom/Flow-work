package com.flowwork.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * 注册请求实体
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度 3-32")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母数字下划线")
    private String username;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.\\w+$", message = "邮箱格式错误")
    private String email;

    private String nickname;

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
