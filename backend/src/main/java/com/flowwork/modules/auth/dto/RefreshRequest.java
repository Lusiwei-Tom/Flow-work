package com.flowwork.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * 刷新请求实体
 */
@Data
public class RefreshRequest {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
    private String deviceId;
}
