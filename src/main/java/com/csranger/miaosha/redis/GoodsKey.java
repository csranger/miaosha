package com.csranger.miaosha.redis;

/**
 * 页面缓存前缀
 */
public class GoodsKey extends BasePrefix {

    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }


    // 以下 2 个实例: 默认缓存过期时间 60s
    public static GoodsKey getGoodsList = new GoodsKey(60, "gl");


    public static GoodsKey getGoodsDetail = new GoodsKey(60, "gd");

}
