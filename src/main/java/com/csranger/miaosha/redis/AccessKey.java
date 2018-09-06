package com.csranger.miaosha.redis;

public class AccessKey extends BasePrefix {

    // 私有化构造器
    public AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    // 列出全部 Access 对象
    // 防止短时间内大量请求某一接口 [键应该是 用户 和 url 一起 ：值指的是访问接口的访问次数]
    public static AccessKey access = new AccessKey(5, "access");  //  保存5s

    public static AccessKey withExpires(int expires) {
        return new AccessKey(expires, "access");
    }

}
