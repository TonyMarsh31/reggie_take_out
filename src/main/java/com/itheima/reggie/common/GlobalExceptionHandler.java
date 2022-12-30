package com.itheima.reggie.common;

import com.itheima.reggie.exception.ObjectContainsNestedProperties;
import com.itheima.reggie.exception.ObjectStillOnStockException;
import com.itheima.reggie.exception.UnclassifiedBusinessException;
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
        log.error("系统异常：插入重复字段到数据库", e);
        if (e.getMessage().contains("Duplicate entry")) {
            String[] message = e.getMessage().split(" ");
            String returnMessage = message[2] + "已存在";
            return R.error(returnMessage);
        }
        return R.error("系统异常");
    }

    @ExceptionHandler(ObjectContainsNestedProperties.class)
    R<String> exceptionHandler(ObjectContainsNestedProperties e) {
        log.error("业务异常：嵌套属性", e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler(ObjectStillOnStockException.class)
    R<String> exceptionHandler(ObjectStillOnStockException e) {
        log.error("业务异常：对象仍在售", e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler(UnclassifiedBusinessException.class)
    R<String> exceptionHandler(UnclassifiedBusinessException e) {
        log.error("未分类的业务异常", e);
        return R.error(e.getMessage());
    }
}
