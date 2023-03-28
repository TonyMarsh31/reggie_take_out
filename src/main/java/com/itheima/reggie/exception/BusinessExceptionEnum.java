package com.itheima.reggie.exception;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public enum BusinessExceptionEnum {
    DUPLICATE_ENTRY(1001, "DUPLICATE_ENTRY"),
    NESTED_PROPERTY(1002, "NESTED_PROPERTY"),
    OBJECT_STILL_ON_STOCK(1003, "OBJECT_STILL_ON_STOCK");

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("exception_messages_zh");
    private final int errorCode;
    private final String errorMessageKey;

    BusinessExceptionEnum(int errorCode, String errorMessageKey) {
        this.errorCode = errorCode;
        this.errorMessageKey = errorMessageKey;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage(Object... params) {
        String pattern = resourceBundle.getString(errorMessageKey);
        return MessageFormat.format(pattern, params);
    }

    public BusinessException toException() {
        return new BusinessException(this);
    }

    public BusinessException toExceptionWithDetail(String detail) {
        return new BusinessException(this, detail);

    }
}
