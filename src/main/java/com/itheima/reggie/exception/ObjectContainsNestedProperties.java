package com.itheima.reggie.exception;

/**
 * 对象包含嵌套属性异常，一般发生在删除操作时，对象包含嵌套属性，导致删除失败
 */
public class ObjectContainsNestedProperties extends RuntimeException {
    public ObjectContainsNestedProperties(String message) {
        super(message);
    }
}
