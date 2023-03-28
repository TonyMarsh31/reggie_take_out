package com.itheima.reggie.exception;

public class BusinessException extends RuntimeException {

    private final int errorCode;
    private final String errorMessage;

    public BusinessException(BusinessExceptionEnum exceptionEnum) {
        super(exceptionEnum.getErrorMessage());
        this.errorCode = exceptionEnum.getErrorCode();
        this.errorMessage = exceptionEnum.getErrorMessage();
    }

    public BusinessException(BusinessExceptionEnum exceptionEnum, String detail) {
        super(exceptionEnum.getErrorMessage() + ": " + detail);
        this.errorCode = exceptionEnum.getErrorCode();
        this.errorMessage = exceptionEnum.getErrorMessage() + ": " + detail;
    }

    // ... 其他getter方法和逻辑
}
