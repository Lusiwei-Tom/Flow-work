package com.flowwork.common.annotation;

import java.lang.annotation.*;

/**
 * 接口权限注解：标注在 Controller 方法或类上，由 PermissionAspect 切面拦截校验
 * 从 JWT 中解析当前用户角色，与注解声明的权限码比对，不匹配则抛出 AccessDeniedException
 * 用法示例：
 *   单权限：@RequirePermission("ai:agent:execute")
 *   多权限：@RequirePermission(value = {"project:read", "project:write"}, logical = Logical.AND)
 */
@Target({ElementType.METHOD, ElementType.TYPE}) // 作用目标：可标注在方法或类上（类级对全部方法生效）
@Retention(RetentionPolicy.RUNTIME) // 保留策略：运行时保留，AOP 切面通过反射读取
@Documented // 生成 Javadoc 时包含该注解
public @interface RequirePermission {
    /** 需要校验的权限码数组，如 "project:read"、"ai:agent:execute" */
    String[] value();

    /**
     * 多权限之间的逻辑关系：
     * AND = 必须全部满足，OR = 满足其一即可（默认 OR）
     */
    Logical logical() default Logical.OR;

    /** 逻辑枚举：AND（全部满足）/ OR（满足其一） */
    enum Logical { AND, OR }
}
