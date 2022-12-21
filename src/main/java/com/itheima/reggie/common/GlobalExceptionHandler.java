package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    R<String> exceptionHandler(SQLIntegrityConstraintViolationException e) {
        log.error("系统异常", e);
        if (e.getMessage().contains("Duplicate entry")) {
            String[] message = e.getMessage().split(" ");
            String returnMessage = message[2] + "已存在";
            return R.error(returnMessage);

        }
        return R.error("系统异常");
    }
}
