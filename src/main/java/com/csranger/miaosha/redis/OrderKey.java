package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> UserKey/OrderKey/MiaoshaUserKey
 *
 *
 *
 * 只有两个属性 prefix(前缀，代表模块)  expireSeconds(存储在 redis 中的过期时间)
 */
public class OrderKey extends BasePrefix {



    public OrderKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

}
