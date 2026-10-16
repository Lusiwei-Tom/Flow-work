package com.flowwork.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
/**
 * 登录响应实体
 */
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserInfoVO userInfo;

    @Data
    @Builder
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private List<String> roles;
        private List<String> permissions;
    }
}
