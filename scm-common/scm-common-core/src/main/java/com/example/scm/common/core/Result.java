package com.example.scm.common.core;

public record Result<T>(boolean success, String code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "200", "OK", data);
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(false, code, message, null);
    }
}
