package com.bytecoach.common.api;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    VALIDATION_ERROR(422, "validation error"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    SERVER_ERROR(500, "server error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

