package com.csranger.miaosha.redis;

/**
 * 接口 -> 抽象类 -> 普通类(代表不同的模块)    模版模式
 * KeyPrefix -> BasePrefix -> UserKey/OrderKey/MiaoshaUserKey
 *
 *
 * 只有两个属性 prefix(前缀，代表模块)  expireSeconds(存储在 redis 中的过期时间)
 */
public interface KeyPrefix {

    int expireSeconds();


    String getPrefix();
}
