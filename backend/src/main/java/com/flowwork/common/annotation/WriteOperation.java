package com.flowwork.common.annotation;

import java.lang.annotation.*;

/**
 * 标记产生业务变更的 Service 方法，被 WriteOperationAspect 拦截，
 * 自动记录 Activity 并推送 WebSocket 事件。
 * 返回值需能解析出 projectId：Task / Project / Map / IdHolder。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WriteOperation {
    /** 动态类型，如 TASK_STATUS_CHANGED */
    String type();

    /** 是否落库 activity；高频事件（拖拽/在线状态）设 false，只走 WebSocket 广播 */
    boolean persist() default true;

    /** 动态文案，支持 {title} 占位（从返回对象属性取值） */
    String content() default "";
}
