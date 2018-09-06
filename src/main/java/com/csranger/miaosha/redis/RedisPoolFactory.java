package com.csranger.miaosha.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * * 根据自定义的配置(redis开头的配置信息例如redis.poolMaxTotal=10)利用 JedisPoolConfig 生成 JedisPool bean (JedisPoolFactory 方法内自己实现)注入spring 容器
 */
@Service     // 使用@EnableConfigurationProperties(RedisConfig.class)+RedisPoolFactory构造器也可以
public class RedisPoolFactory {

    @Autowired
    private RedisConfig redisConfig;

    // 将 jedis pool Bean 注入到 spring 容器
    @Bean
    public JedisPool jedisPoolFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(redisConfig.getPoolMaxTotal());
        jedisPoolConfig.setMaxIdle(redisConfig.getPoolMaxIdle());
        jedisPoolConfig.setMaxWaitMillis(redisConfig.getPoolMaxWait() * 1000);
        JedisPool jp = new JedisPool(jedisPoolConfig, redisConfig.getHost(), redisConfig.getPort(),
                redisConfig.getTimeout() * 1000, redisConfig.getPassword(), 0);
        return jp;
    }


}
