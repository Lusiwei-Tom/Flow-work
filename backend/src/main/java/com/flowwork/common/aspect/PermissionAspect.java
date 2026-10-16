package com.flowwork.common.aspect;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 动态权限切面：拦截 @RequirePermission，从 SecurityContext 取 userId，
 * 走 PermissionService（Redis 缓存）校验
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;//

    @Before("@annotation(rp)")
    public void check(RequirePermission rp) {
        Long userId = SecurityUtils.getCurrentUserId(); // 从 SecurityContext 取 userId
        permissionService.check(userId, rp.value(), // 校验用户权限
                rp.logical() == RequirePermission.Logical.AND
                        ? PermissionService.RequirePermMode.AND // 逻辑与校验
                        : PermissionService.RequirePermMode.OR);// 逻辑或校验
    }
}
