package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> UserKey/OrderKey/MiaoshaUserKey
 * 订单缓存前缀
 *
 * 只有两个属性 prefix(前缀，代表模块)  expireSeconds(存储在 redis 中的过期时间)
 */
public class OrderKey extends BasePrefix {


    // 私有化构造器，控制 订单缓存前缀 对象的数量,永不过期
    private OrderKey(String prefix) {
        super(prefix);
    }


    // 列出所以 订单缓存前缀 对象
    public static OrderKey getMiaoshaOrderByUserIdGoodsId = new OrderKey("MiaoshaOrder");

}
