package com.flowwork.common.exception;

import com.flowwork.common.result.ResultCode;
import lombok.Getter;

@Getter
// 业务异常类，继承 RuntimeException（运行时异常），用于表示业务逻辑错误
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());// 调用父类构造函数，设置异常消息
        this.code = rc.getCode();
    }

    public BusinessException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
