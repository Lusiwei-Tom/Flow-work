package com.flowwork.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;

/**
 * 统一响应包装类：所有 Controller 接口返回值统一用 Result 包装
 * 结构：{ code, message, data, traceId }
 * - code：业务状态码（0=成功，非 0=失败，详见 ResultCode 枚举）
 * - message：提示信息
 * - data：业务数据（泛型 T）
 * - traceId：链路追踪 ID，从 MDC 中读取，便于日志关联排查
 */
@Data // Lombok：自动生成 getter/setter/toString/equals/hashCode
@JsonInclude(JsonInclude.Include.NON_NULL) // Jackson 序列化时跳过 null 字段：null 字段不出现在 JSON 中，减小响应体积
public class Result<T> {
    /** 业务状态码：0=成功，非 0=失败 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;
    /** 链路追踪 ID（从 MDC 读取，日志排查用） */
    private String traceId;

    /**
     * 成功（无数据）：code=0，data=null
     */
    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS, null, null);
    }

    /**
     * 成功（带数据）：code=0，data=传入对象
     */
    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS, null, data);
    }

    /**
     * 失败（使用枚举默认消息）：code=枚举码，message=枚举默认消息
     */
    public static <T> Result<T> fail(ResultCode rc) {
        return build(rc, rc.getMessage(), null);
    }

    /**
     * 失败（自定义 code 和 message）：code 与 message 完全自定义
     */
    public static <T> Result<T> fail(int code, String message) {
        return build(code, message, null);
    }

    /**
     * 失败（枚举码 + 自定义 message）：code=枚举码，message 覆盖枚举默认消息
     */
    public static <T> Result<T> fail(ResultCode rc, String message) {
        return build(rc, message, null);
    }

    /**
     * 内部构建：基于 ResultCode 枚举，message 为 null 时回退到枚举默认消息
     */
    private static <T> Result<T> build(ResultCode rc, String message, T data) {
        return build(rc.getCode(), message != null ? message : rc.getMessage(), data);
    }

    /**
     * 内部构建：组装 Result 对象，traceId 从 MDC 读取
     */
    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.traceId = MDC.get("traceId");
        return r;
    }
}
