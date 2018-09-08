package com.csranger.miaosha.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置一些 Bean：消息队列Queue + 交换机Exchange + 队列和交换机之间的绑定
 */
@Configuration
public class MQConfig {

    // 队列名，交换机名 全部列出
    public static final String DIRECT_QUEUE = "direct.queue";        // direct 队列名

    public static final String TOPIC_QUEUE1 = "topic.queue1";        // Topic 队列名
    public static final String TOPIC_QUEUE2 = "topic.queue2";
    public static final String TOPIC_EXCHANGE = "topicExchange";     // Topic 交换机名

    public static final String FANOUT_EXCHANGE = "fanoutExchange";   // Fanout 广播的交换机名

    public static final String HEADERS_QUEUE = "headers.queue";      // Headers 队列名
    public static final String HEADERS_EXCHANGE = "headersExchange"; // Headers 广播的交换机名

    public static final String MIAOSHA_QUEUE = "miaosha.queue";       // 秒杀 queue 队列


    /**
     * 1. Direct 模式
     */

    /**
     * Direct 队列
     */
    @Bean
    public Queue directQueue() {
        return new Queue(DIRECT_QUEUE, true);
    }

    /**
     * 秒杀队列Queue， Direct 模式
     */
    @Bean
    public Queue miaoshaQueue() {
        return new Queue(MIAOSHA_QUEUE, true);
    }


    /**
     * 2. Topic 模式 交换机Exchange
     * 先把消息放入 Exchange
     */

    /**
     * Topic 队列
     */
    @Bean
    public Queue topicQueue1() {
        return new Queue(TOPIC_QUEUE1, true);
    }

    @Bean
    public Queue topicQueue2() {
        return new Queue(TOPIC_QUEUE2, true);
    }

    /**
     * Topic交换机Exchange
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    /**
     * Topic交换机和Queue进行绑定
     * 向 交换机 传信息时还需一个 routingKey 参数，如果放"topic.key1"值，两个绑定均满足，所以这个信息会放到两个队列中 topicQueue1 和 topicQueue2
     * 如果放"topic.key2"值，只有topicBinding2绑定满足，所以这个信息只会放入 topicQueue2 队列中
     */
    @Bean
    public Binding topicBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with("topic.key1");
    }

    @Bean
    public Binding topicBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with("topic.#");
    }


    /**
     * 3. Fanout 模式(广播模式) 交换机Exchange
     * 可以发给多个Queue，没有key的限制
     * 使用Topic模式的 队列 Queue
     */

    /**
     * Fanout 交换机 Exchange
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE);
    }

    /**
     * Fanout 交换机和Queue进行绑定
     */
    @Bean
    public Binding fanoutBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(fanoutExchange());
    }

    @Bean
    public Binding fanoutBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(fanoutExchange());
    }


    /**
     * 4. Headers 模式
     *
     */

    /**
     * Headers 队列
     */
    @Bean
    public Queue headersQueue() {
        return new Queue(HEADERS_QUEUE, true);
    }

    /**
     * Headers 交换机Exchange
     */
    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(HEADERS_EXCHANGE);
    }

    /**
     * Headers 队列与交换节进行绑定
     */
    @Bean
    public Binding headersBinding() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("headers1", "value1");
        map.put("headers2", "value2");
        return BindingBuilder.bind(headersQueue()).to(headersExchange()).whereAll(map).match();
    }
}


















