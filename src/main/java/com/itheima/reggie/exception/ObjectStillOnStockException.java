package com.itheima.reggie.exception;

/**
 * 对象目前在库(菜品或套餐在售卖中),无法进行进一步操作。该操作通常是删除操作
 */
public class ObjectStillOnStockException extends RuntimeException {
    public ObjectStillOnStockException(String message) {
        super(message);
    }

    public ObjectStillOnStockException() {
        super("有操作对象仍在售卖中,无法执行删除,请先停售商品");
    }
}
