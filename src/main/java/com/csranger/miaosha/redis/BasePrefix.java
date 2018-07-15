package com.csranger.miaosha.redis;

public abstract class BasePrefix implements KeyPrefix {

    private int expireSeconds;    // 过期时间

    private String prefix;



    // public 构造器，但是抽象类不可用于new一个对象
    public BasePrefix(String prefix) {
        this(0, prefix);   // 0表示永不过期
    }

    public BasePrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }




    @Override
    public int expireSeconds() {    // 默认0，代表永不过期
        return expireSeconds;
    }

    @Override
    public String getPrefix() {
        String className = getClass().getName();
        return className + ":" + prefix;
    }
}
