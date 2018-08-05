package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> OrderKey/MiaoshaUserKey/GoodsKey/MiaoshaKey
 *
 * 是否还有秒杀商品库存前缀    goodsId -> true/对应商品卖完
 */
public class MiaoshaKey extends BasePrefix {

    // 私有化构造器
    private MiaoshaKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    // 秒杀商品库存前缀 对象
    public static MiaoshaKey isGoodsOver = new MiaoshaKey(0, "goodsOver");



}
