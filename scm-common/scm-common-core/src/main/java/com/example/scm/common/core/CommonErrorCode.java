package com.example.scm.common.core;

public enum CommonErrorCode {
    BAD_REQUEST("400", "Bad request"),
    NOT_FOUND("404", "Resource not found"),
    INTERNAL_ERROR("500", "Internal server error");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
