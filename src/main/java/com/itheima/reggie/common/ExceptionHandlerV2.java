package com.itheima.reggie.common;

import com.itheima.reggie.exception.ObjectContainsNestedProperties;
import com.itheima.reggie.exception.ObjectStillOnStockException;
import com.itheima.reggie.exception.UnclassifiedBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class ExceptionHandlerV2 {

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    R<String> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        return handleException("系统异常：插入重复字段到数据库", e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            ObjectContainsNestedProperties.class,
            ObjectStillOnStockException.class,
            UnclassifiedBusinessException.class})
    R<String> handleBusinessExceptions(RuntimeException e) {
        return handleException("业务异常", e);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    R<String> handleGeneralException(Exception e) {
        return handleException("未捕获的异常", e, "服务器内部错误，请联系管理员");
    }

    private R<String> handleException(String logMessage, Exception e) {
        log.error(logMessage, e);
        return R.error(e.getMessage());
    }

    private R<String> handleException(String logMessage, Exception e, String defaultMessage) {
        log.error(logMessage, e);
        return R.error(defaultMessage);
    }
}
