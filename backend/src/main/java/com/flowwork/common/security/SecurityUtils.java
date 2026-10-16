package com.flowwork.common.security;

import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类：从 SecurityContextHolder 提取当前登录用户 ID
 * 数据来源：JwtAuthenticationFilter 解析 token 后填入 SecurityContext 的 principal（即 userId）
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户 ID，未登录则抛 UNAUTHORIZED 异常
     * auth.getName() 即 principal 字符串值（JwtAuthenticationFilter 中设置为 userId 字符串）
     */
    public static Long getCurrentUserId() {
        // 从 SecurityContextHolder 取当前认证对象
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 未认证 / 匿名用户 → 抛 401
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return Long.valueOf(auth.getName());
    }

    /**
     * 获取当前用户 ID，未登录时返回 null（不抛异常）
     * 适用于非强制登录的场景：如 WriteOperationAspect 中区分人工/AI 操作者
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
