package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
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

    // 进行秒杀：(1)减库存 -> (2)生成订单 -> (3)数据库插入秒杀订单
    @Transactional
    public OrderInfo miaosha(MiaoshaUser user, GoodsVO goods) {
        // (1)减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
        goodsService.reduceStock(goods);
        // (2)生成订单:即在 order_info 和 miaosha_order 表中插入一条记录
        // miaosha_order 是 order_info 子集，只包含参加秒杀活动的商品订单
        return orderService.createOrder(user, goods);
    }



}
