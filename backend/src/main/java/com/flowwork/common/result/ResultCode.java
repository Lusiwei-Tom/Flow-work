package com.flowwork.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(10001, "参数校验失败"),
    NOT_FOUND(10002, "资源不存在"),
    TOKEN_EXPIRED(11001, "Token 已过期"),
    TOKEN_INVALID(11002, "Token 无效"),
    ACCOUNT_LOCKED(11003, "账号已锁定"),
    LOGIN_FAIL(11004, "用户名或密码错误"),
    UNAUTHORIZED(11005, "未登录或登录已失效"),
    FORBIDDEN(12001, "无权限访问"),
    ROLE_NOT_EXIST(12002, "角色不存在"),
    BIZ_CONFLICT(13001, "业务冲突"),
    DUPLICATE_NAME(13002, "名称重复"),
    CYCLE_DEPENDENCY(13003, "存在循环依赖"),
    LIMIT_EXCEEDED(13004, "超出数量限制"),
    PROJECT_ARCHIVED(13005, "项目已归档，只读"),
    LOCK_CONFLICT(13006, "资源正被他人操作，请刷新重试"),
    INVITE_CODE_INVALID(13007, "邀请码无效或已停用"),
    INVITE_CODE_EXPIRED(13008, "邀请码已过期或使用次数已用尽"),
    SERVER_ERROR(20001, "服务繁忙，请稍后再试"),
    DB_ERROR(20002, "数据访问异常"),
    REDIS_ERROR(20003, "缓存服务异常"),
    AI_TIMEOUT(20004, "AI 服务响应超时"),
    AI_DISABLED(20005, "AI 功能未配置，请联系管理员"),
    RATE_LIMITED(20006, "请求过于频繁，请稍后再试"),
    TOOL_CALL_FAIL(21001, "AI 工具调用失败"),
    STRUCTURED_OUTPUT_FAIL(21002, "AI 结构化输出校验失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
