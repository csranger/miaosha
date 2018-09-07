package com.csranger.miaosha.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义的，和原生的区别开
 * 或者这里不使用 @Component 注解，直接在 RedisService 类上的进行@EnableConfigurationProperties(RedisConfig.class)注解，
 * 仿照house项目里的HttpClientAutoConfiguration类
 */
@Component                               // 交由容器管理
@ConfigurationProperties(prefix = "redis")    // 读取配置文件(application.properties)里以 redis 开头的配置
@Data                                        // setter getter 方法
public class RedisConfig {
    private String host;
    private int port;
    private int timeout;           // 秒
    private String password;
    private int poolMaxTotal;
    private int poolMaxIdle;
    private int poolMaxWait;        // 秒
}
