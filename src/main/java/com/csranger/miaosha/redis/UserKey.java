package com.csranger.miaosha.redis;

public class UserKey extends BasePrefix {
    private UserKey(String prefix) {
        super(prefix);
    }

    // Userkey 根据 id 的缓存
    // 类变量
    public static UserKey getById = new UserKey("id");

    // Userkey 根据 name 的缓存
    public static UserKey getByName = new UserKey("name");
}
