package com.community.homeservice.common;

/**
 * 业务异常：用于向用户展示可读的失败原因。
 * <p>默认按 400 处理；资源不存在用 404，越权访问用 403，便于前端与监控区分。</p>
 */
public class BusinessException extends RuntimeException {

    private final int status;

    public BusinessException(String message) {
        this(message, 400);
    }

    public BusinessException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(message, 404);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, 403);
    }
}
