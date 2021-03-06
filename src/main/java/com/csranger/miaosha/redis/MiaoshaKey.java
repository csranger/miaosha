package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> OrderKey/MiaoshaUserKey/GoodsKey/MiaoshaKey
 * <p>
 * 是否还有秒杀商品库存前缀    goodsId -> true/对应商品卖完
 */
public class MiaoshaKey extends BasePrefix {

    // 私有化构造器
    private MiaoshaKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    // 秒杀商品库存前缀 对象
    public static MiaoshaKey isGoodsOver = new MiaoshaKey(0, "isGoodsOver");
    // 执行秒杀时为隐藏秒杀地址，请求秒杀时需带一个随机数，将这个随机数存在redis
    public static MiaoshaKey getMiaoshaPath = new MiaoshaKey(60, "getMiaoshaPath");  // 60s
    // 验证码的答案缓存 前缀
    public static MiaoshaKey getMiaoshaVerifyCode = new MiaoshaKey(300, "getMiaoshaVerifyCode");  // 5min

}
