package com.csranger.miaosha.access;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 定义一个注解，最为关键的是实现一个拦截器来处理这个注解
 */
@Retention(RUNTIME)             // 注解的的存活时间 注解可以保留到程序运行的时候
@Target(METHOD)                 // @Target 指定了注解运用的地方 可以给方法进行注解
public @interface AccessLimit {
    int seconds();                           // 时间

    int maxCounts();                         // 在指定时间内的最大访问次数

    boolean needLogin() default true;      // 是否需要登录 默认需要登录
}
