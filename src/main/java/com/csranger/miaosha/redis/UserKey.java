package com.csranger.miaosha.redis;

/**
 * KeyPrefix -> BasePrefix -> UserKey/OrderKey/MiaoshaUserKey
 *
 *
 * 只有两个属性 prefix(前缀，代表模块)  expireSeconds(存储在 redis 中的过期时间)
 */
public class UserKey extends BasePrefix {


    // 私有构造器，限定只有两个对象
    private UserKey(String prefix) {
        super(prefix);
    }




    // 此类的 2 个对象使用静态方法列出来
    public static UserKey getById = new UserKey("id");

    public static UserKey getByName = new UserKey("name");
}
