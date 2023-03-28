package com.itheima.reggie.common;

/**
 * 基于ThreadLocal封装工具类，用于保存和获取当前登录用户id
 * ThreadLocal是Java中一个线程级别的变量，每个线程都可以存储一个ThreadLocal数据副本，该副本仅由该线程读取和写入，并且在其它线程中不可见。
 * 在本场景下，需要将一些用户id在多个方法之间传递。使用ThreadLocal可以在当前线程中携带这些变量，不必担心方法间如何传递它们。
 */
public class BaseContext {
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 获取值
     *
     * @return 用户id
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 设置值
     *
     * @param id 用户id
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }
}