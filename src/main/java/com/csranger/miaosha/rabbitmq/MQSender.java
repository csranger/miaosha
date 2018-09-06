package com.csranger.miaosha.rabbitmq;

import com.csranger.miaosha.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 向指定 队列 或 交换机发送数据
 */
@Service
public class MQSender {

    private static Logger log = LoggerFactory.getLogger(MQSender.class);

    // 操作 mq 的帮助类
    @Autowired
    private AmqpTemplate amqpTemplate;


    /**
     * 1. Direct 模式
     * 指定向名为 DIRECT_QUEUE 的队列发送数据
     */
    public void sendDirect(Object message) {
        // 对象转化成字符串，之前 redis 中写过 beanToString 方法，利用 fastjson 依赖
        String msg = RedisService.beanToString(message);
        log.info("send message: " + message);
        amqpTemplate.convertAndSend(MQConfig.DIRECT_QUEUE, msg);   // 指定发送到哪个 Queue
    }

    /**
     * 2. Topic 模式 交换机Exchange
     * 指定向名为 TOPIC_EXCHANGE 的交换机发送数据(因为交换机是和队列绑定在一起的)
     */
    public void sendTopic(Object message) {
        // 对象转化成字符串，之前 redis 中写过 beanToString 方法，利用 fastjson 依赖
        String msg = RedisService.beanToString(message);
        log.info("send topic message: " + message);
        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key1", msg + 1);
        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key2", msg + 2);
    }


    /**
     * 3. Fanout 模式(广播模式) 交换机Exchange
     */
    public void sendFanout(Object message) {
        String msg = RedisService.beanToString(message);
        log.info("send fanout message: " + message);
        amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE, "", msg);
    }


    /**
     * 4. Headers 模式 交换机Exchange
     */
    public void sendHeaders(Object message) {
        String msg = RedisService.beanToString(message);
        log.info("send headers message: " + message);
        //  传 Message 对象
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("headers1", "value1");
        messageProperties.setHeader("headers2", "value2");
        Message obj = new Message(msg.getBytes(), messageProperties);
        amqpTemplate.convertAndSend(MQConfig.HEADERS_EXCHANGE, "", obj);
    }


    // 使用 Direct 模式发送秒杀信息
    public void sendMiaoshaMessage(MiaoshaMessage miaoshaMessage) {
        // 对象转化成字符串，之前 redis 中写过 beanToString 方法，利用 fastjson 依赖
        String message = RedisService.beanToString(miaoshaMessage);
        log.info("send message: " + message);
        // 放入名为 DIRECT_QUEUE 的队列
        amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE, message);
    }

}
