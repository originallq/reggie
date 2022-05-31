package com.itheima.common;

/*
    作用域为同一个线程,不同的线程不能获取存储的id值
    基于ThreadLocal封装的工具类(都是静态方法),用于保存和获取当前登录用户的id
*/
public class BaseContext {
    //Long类型,用于存储用户id
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    //存储id方法,一般在过滤器中使用
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }
    //取出id方法,一般在公共字段填充方法中使用
    public static Long getCurrentId(){
        return threadLocal.get();
    }
}
