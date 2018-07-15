package com.csranger.miaosha.redis;

/**
 * 接口 -> 抽象类 -> 普通类(代表不同的模块)    模版模式
 * KeyPrefix -> BasePrefix -> UserKey/OrderKey
 */
public interface KeyPrefix {

    int expireSeconds();


    // 返回前缀：代表 redis 存储的 key-value 属于哪个模块
    String getPrefix();
}
