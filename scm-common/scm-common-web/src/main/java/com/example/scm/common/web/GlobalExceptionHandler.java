package com.example.scm.common.web;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一返回响应体，并把异常堆栈输出到控制台。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.error("Business exception, uri={}, method={}, code={}, message={}",
                request.getRequestURI(), request.getMethod(), ex.getCode(), ex.getMessage(), ex);
        return Result.failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.error("Bad request exception, uri={}, method={}, message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage(), ex);
        return Result.failure(CommonErrorCode.BAD_REQUEST.code(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception, uri={}, method={}, message={}",
                request.getRequestURI(), request.getMethod(), ex.getMessage(), ex);
        return Result.failure(CommonErrorCode.INTERNAL_ERROR.code(), ex.getMessage());
    }
}
