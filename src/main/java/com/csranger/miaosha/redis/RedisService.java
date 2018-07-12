package com.csranger.miaosha.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 此配置文件最终目的是希望生成 Jedis 的 bean，提供 redis 服务例如： jedis.set("foo", ""bar) String value = jedis.get("foo")
 * 2。接着在 get put 方法内使用 JedisPool 生成 Jedis Bean，通过 jedis 对象就可以进行 set get 了
 */
@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    /**
     * 1。从 redis 服务器中获取
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T get(String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = jedis.get(key);
            T t = stringToBean(str, clazz);
            return t;
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 将字符串转化成一个 Bean 对象
     * @param s
     * @return
     */
    public <T> T stringToBean(String s, Class<T> clazz) {
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
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    public <T> boolean set(String key, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if (str == null || str.length() <= 0) {
                return false;
            }
            jedis.set(key, str);
            return true;
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 将任意类型转化成字符串
     * @param value
     * @param <T>
     * @return
     */
    private <T> String beanToString(T value) {
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
        } else {    // 其他类型认为它是一个 Bean，利用 fastjson 转换成 String
            return JSON.toJSONString(value);
        }
    }



    //
    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();     // 返回到连接池中
        }
    }

}
