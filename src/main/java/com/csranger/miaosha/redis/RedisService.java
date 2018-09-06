package com.csranger.miaosha.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 此配置文件最终目的是希望生成 Jedis 的 bean，提供 redis 服务例如： jedis.set("foo", ""bar) String value = jedis.get("foo")
 * 接着在 get put 方法内使用 JedisPool 生成 Jedis Bean，通过 jedis 对象就可以进行 set get 了
 */
@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;


    /**
     * 1。从 redis 服务器中获取对象，例如User对象，键可以是id，name等
     */
    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // realKey
            String realKey = prefix.getPrefix() + key;
            String str = jedis.get(realKey);
            T t = stringToBean(str, clazz);
            return t;
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 将 字符串 转化成一个 对象
     * 作为工具类，rabbitmq 中也会用到
     */
    public static <T> T stringToBean(String s, Class<T> clazz) {
        if (s == null || s.length() <= 0 || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(s);
        } else if (clazz == String.class) {
            return (T) s;
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(s);
        } else {    // 其他类型认为它是一个 Bean，利用 fastjson 转换成 String
            return JSON.toJavaObject(JSON.parseObject(s), clazz);
        }
    }


    /**
     * 2。使用 fastjson 将一个 Bean 对象转化成 String 写入到 redis 中
     */
    public <T> boolean set(KeyPrefix prefix, String key, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if (str == null || str.length() <= 0) {
                return false;
            }
            // realKey
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();   // 过期时间
            if (seconds <= 0) {    // 意味着永不过期
                jedis.set(realKey, str);
            } else {                // 设置了过期时间，存在redis时使用 setex 方法也传递一个过期时间
                jedis.setex(realKey, seconds, str);
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 将 对象 转化成 字符串
     * 作为工具类，rabbitmq 中也会用到
     */
    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "" + value;
        } else if (clazz == String.class) {
            return (String) value;
        } else if (clazz == long.class || clazz == Long.class) {
            return "" + value;
        } else {    // 其他类型认为它是一个 Bean，利用 fastjson 转换成 String：例如 MiaoshaUser 对象
            return JSON.toJSONString(value);
        }
    }


    // 两个方法的工具方法，释放 Jedis 到 JedisPool
    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();     // 返回到连接池中
        }
    }

    /**
     * 3. 判断一个 key 是否存在于 redis 中
     */
    public <T> boolean exists(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // realKey
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 4. increase
     */
    public <T> Long incr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // realKey
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 5. decrease
     */
    public <T> Long decr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // realKey
            String realKey = prefix.getPrefix() + key;
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 6. delete 删除指定键对应的缓存
     */
    public boolean delete(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // realKey
            String realKey = prefix.getPrefix() + key;
            long ret = jedis.del(realKey);
            return ret > 0;
        } finally {
            returnToPool(jedis);
        }
    }

}
