package com.flowwork.common.annotation;

import java.lang.annotation.*;

/**
 * AI 工具方法标记注解
 * 标注在 @Tool 工具方法上，由 AiToolAuditAspect 切面拦截：
 * 1. 方法执行前：设置 AiContextHolder（项目/用户上下文）
 * 2. 方法执行后：记录 ai_tool_call_log 审计日志（调用参数、返回值、耗时、异常）
 * 用法示例：@AiTool("查询任务") public List<Task> queryTasks(...)
 */
@Target(ElementType.METHOD) // 作用目标：仅可标注在方法上
@Retention(RetentionPolicy.RUNTIME) // 保留策略：运行时保留，反射可读取（AOP 切面依赖此策略）
@Documented // 生成 Javadoc 时包含该注解
public @interface AiTool {
    /** 工具名称/描述，用于审计日志中标识该工具，缺省为空 
     * 在 Java 注解中，属性是以无参方法的形式来声明的，方法名就是属性名，返回值就是属性值。
     * 这种形式的属性在注解中是常用的，因为注解的属性值需要在编译时确定，而不能在运行时确定
    */
    String value() default "";
}
