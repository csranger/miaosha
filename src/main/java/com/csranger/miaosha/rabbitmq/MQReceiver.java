package com.csranger.miaosha.rabbitmq;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.service.GoodsService;
import com.csranger.miaosha.service.MiaoshaService;
import com.csranger.miaosha.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 从队列中取出数据
 */
@Service
public class MQReceiver {

    private static Logger log = LoggerFactory.getLogger(MQReceiver.class);

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MiaoshaService miaoshaService;



    // 接收 direct.queue 队列发送的消息
    @RabbitListener(queues = MQConfig.DIRECT_QUEUE)
    public void receiveDirectQueue(String message) {
        log.info("receive message: " + message);
        log.info("receive message: " + message);
        log.info("receive message: " + message);
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



    // 接收 miaosha.queue 队列发送的消息
    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void receiveMiaoshaQueue(String message) {
        log.info("receive miaosha message: " + message);
        // 4. 秒杀请求出队，生成订单，减少库存：将收到 MiaoshaMessage 消息还原成对象，获取秒杀信息
        MiaoshaMessage miaoshaMessage = RedisService.stringToBean(message, MiaoshaMessage.class);
        MiaoshaUser miaoshaUser = miaoshaMessage.getMiaoshaUser();
        long goodsId = miaoshaMessage.getGoodsId();
        // 判断库存，这里访问了数据库，这一步很少请求可以进来；没库存就返回，什么都不做
        log.info("从rabbirmq队列中取出秒杀信息，判断此商品是否还有库存");
        GoodsVO goodsVO = goodsService.getGoodsVOByGoodsId(goodsId);
        int stock = goodsVO.getStockCount();      // 注意这里是 getStockCount 不是 getGoodsStock
        if (stock < 0) {
            return;
        }
        // 有库存判断是否重复秒杀
        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
        if (miaoshaOrder != null) {
            return;
        }
        // 生成秒杀订单
        miaoshaService.miaosha(miaoshaUser, goodsVO);
    }
}
