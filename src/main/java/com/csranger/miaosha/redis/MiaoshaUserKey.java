package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> OrderKey/MiaoshaUserKey/GoodsKey
 * 对象缓存前缀
 * <p>
 * 只有两个属性 prefix(前缀，代表模块)  expireSeconds(存储在 redis 中的过期时间)
 */
public class MiaoshaUserKey extends BasePrefix {


    public static final int TOKEN_EXPIRE = 3600 * 24 * 2;   // 2d


    // 私有化构造器，控制 对象缓存前缀 对象的数量
    private MiaoshaUserKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }


    // 列出所以 对象缓存前缀 对象
    // 此 MiaoshaUserKey 类的对象 token 使用静态方法列出来: 过期时间 3600 * 24 * 2 ，前缀 "tk"
    public static MiaoshaUserKey token = new MiaoshaUserKey(TOKEN_EXPIRE, "tk");
    public static MiaoshaUserKey getById = new MiaoshaUserKey(0, "id");  // 过期时间设为0，意味着永久有效


    // 测试
    // 加深理解 MiaoshaUserKey 唯一对象 MiaoshaUserKey.token
    public static void main(String[] args) {
        MiaoshaUserKey miaoshaUserKey = MiaoshaUserKey.token;
        System.out.println(miaoshaUserKey.getPrefix());
        System.out.println(miaoshaUserKey.expireSeconds());
    }
}
