package com.csranger.miaosha.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 从队列中取出数据
 */
@Service
public class MQReceiver {

    private static Logger log = LoggerFactory.getLogger(MQReceiver.class);



    // 接收 direct.queue 队列发送的消息
    @RabbitListener(queues = MQConfig.DIRECT_QUEUE)
    public void receiveDirectQueue(String message) {
        log.info("receive message: " + message);
    }


    // 接收 topic.queue1 队列发送的消息
    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
    public void receiveTopicQueue1(String message) {
        log.info("receive queue1 message: " + message);
    }



    // 接收 topic.queue2 队列发送的消息
    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
    public void receiveTopicQueue2(String message) {
        log.info("receive queue2 message: " + message);
    }



    // 接收 headers.queue 队列发送的消息
    @RabbitListener(queues = MQConfig.HEADERS_QUEUE)
    public void receiveHeadersQueue(byte[] message) {
        log.info("receive headers message: " + new String(message));
    }
}
