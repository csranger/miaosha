package com.csranger.miaosha.redis;

/**
 * 只有两个属性 prefix(前缀，代表模块)  expireSeconds(存储在 redis 中的过期时间)
 */
public abstract class BasePrefix implements KeyPrefix {

    private int expireSeconds;    // redis 存储有效期，单位 s, 在MiaoshaService里让其等于 cookie 有限期

    private String prefix;


    // public 构造器，但是抽象类不可用于new一个对象
    public BasePrefix(String prefix) {
        this(0, prefix);   // 0表示永不过期
    }

    public BasePrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }


    // 获取两个属性的值的方法
    @Override
    public int expireSeconds() {    // 默认0，代表永不过期
        return expireSeconds;
    }

    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();   // 不会像 getName() 方法一样加上包名
        return className + ":" + prefix;
    }
}
