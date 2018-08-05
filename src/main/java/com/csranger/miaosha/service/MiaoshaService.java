package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import com.csranger.miaosha.redis.MiaoshaKey;
import com.csranger.miaosha.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 将执行秒杀操作放入，进行事务管理
 * (1)减库存 -> (2)下订单 -> (3)数据库插入秒杀订单
 */
@Service
public class MiaoshaService {


    /**
     * 这里引入 GoodsService 是为了使用 GoodsDao，因为一般提倡在自己的Service下引入自己的Dao(比如说在GoodsService引入GoodsDao)，
     * 所以这里引入 GoodsService 解决
     */
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisService redisService;

    // 进行秒杀：(1)减库存 -> (2)数据库插入生成的秒杀订单与订单
    @Transactional
    public OrderInfo miaosha(MiaoshaUser user, GoodsVO goods) {
        // (1)减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
        // 虽然 MiaoshaController 里已经在 MiaoshaController 里检查了是否有库存，但可能发生多个请求同时检查库存，例如库存为2，同时有10个请求，
        // 这时有库存，但实际数据库操作就会减库存失败8次，因此这8次不可以生成订单，因为库存没了
        boolean success = goodsService.reduceStock(goods);
        // (2)数据库插入生成的秒杀订单与订单:即在 order_info 和 miaosha_order 表中插入一条记录
        // miaosha_order 是 order_info 子集，只包含参加秒杀活动的商品订单
        if (success) {
            return orderService.createOrder(user, goods);
        } else {
            // 库存没了，标记该商品秒杀卖完了：在redis中插入 goodsId -> true(商品卖完)
            // 重要，当客户端请求秒杀结果时，用于区分是在排队还是秒杀失败
            setGoodsOver(goods.getId());
            return null;    // 我感觉这里应该抛出异常更好，throw new GlobalException(CodeMsg.MIAOSHA_OVER)
        }
    }

    /**
     * orderId : 秒杀成功，数据库中可以查到订单，返回订单id即可
     * -1      : 秒杀失败，意味着商品秒杀卖完了
     * 0       : 排队中，客户端继续轮询，再次查询秒杀结果
     * 注意点：秒杀失败和排队中两种情况下均查不到订单，如何区分开来？
     * 查不到订单情况下，如果该商品没有库存，说明秒杀失败，如果该商品有库存，说明还在队列中
     */
    public long getMiaoshaResult(long userId, long goodsId) {
        // 从数据库里查是否生成了订单
        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
        if (miaoshaOrder != null) {   // 生成了订单 秒杀成功
            return miaoshaOrder.getOrderId();
        } else {
            // 数据库查不到订单原因 1. 秒杀失败 2. 在 rabbitmq 队列中，排队中，还没执行到   如何区分？
            // ajax 发起里查询秒杀结果的请求，说明秒杀请求已经提交到rabbitmq队列，如果还有此商品的库存，则应该是在排队中，反之秒杀失败
            boolean isOver = getGoodsOver(goodsId);     // true 表示没库存了即秒杀失败
            if (isOver) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    // 在 redis 中标记此商品卖完 即 goodsId -> true
    private void setGoodsOver(long goodsId) {
        redisService.set(MiaoshaKey.isGoodsOver, "" + goodsId, true);
    }

    // 查看 redis 中是否标记此商品卖完
    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(MiaoshaKey.isGoodsOver, "" + goodsId);
    }



}
