package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.dao.OrderDao;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {

    // 一般提倡在自己的Service下引入自己的Dao(比如说在GoodsService引入GoodsDao)，其他的Dao通过引入对应的Service解决
    @Autowired
    private OrderDao orderDao;


    /**
     * 此用户有没有对此商品秒杀成功过，有则返回 秒杀订单
     */
    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
        return orderDao.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
    }

    /**
     * 生成订单:即在 order_info 和 miaosha_order 表中插入一条记录
     */
    @Transactional
    public OrderInfo createOrder(MiaoshaUser miaoshaUser, GoodsVO good) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(miaoshaUser.getId());
        orderInfo.setGoodsId(good.getId());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsName(good.getGoodsName());
        orderInfo.setGoodsCount(1);   // 购买数量
        orderInfo.setGoodsPrice(good.getGoodsPrice());
        orderInfo.setOrderChannel(1);  // 1-pc 2-android 3-ios
        orderInfo.setStatus(0);    // 0-新建未支付 1-已支付 2-已发货 3-已收获 4-已退款 5-已完成
        orderInfo.setCreateDate(new Date());
        long orderId = orderDao.insert(orderInfo);
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setUserId(miaoshaUser.getId());
        miaoshaOrder.setOrderId(orderId);
        miaoshaOrder.setGoodsId(good.getId());
        orderDao.insertMiaoshaOrder(miaoshaOrder);

        return orderInfo;
    }



}
