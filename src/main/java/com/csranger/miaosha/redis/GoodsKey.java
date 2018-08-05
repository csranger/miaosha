package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> OrderKey/MiaoshaUserKey/GoodsKey/MiaoshaKey
 *
 * 页面缓存前缀
 */
public class GoodsKey extends BasePrefix {

    // 私有化构造器，控制 页面缓存前缀 对象的数量
    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }


    // 列出所以 页面缓存前缀 对象
    // 以下 2 个实例: 默认缓存过期时间 60s
    public static GoodsKey getGoodsList = new GoodsKey(60, "gl");
    public static GoodsKey getGoodsDetail = new GoodsKey(60, "gd");
    public static GoodsKey getMiaoshaGoodsStock = new GoodsKey(0, "gs");

}
