package com.csranger.miaosha.controller;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.VO.OrderDetailVO;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.GoodsService;
import com.csranger.miaosha.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);


    @Autowired
    private OrderService orderService;

    @Autowired
    private GoodsService goodsService;

    //
    @RequestMapping(value = "/detail")
    @ResponseBody
    public Result<OrderDetailVO> info(MiaoshaUser miaoshaUser, @RequestParam("orderId") long orderId) {
        logger.info("正在处理请求订单详情页 order/info 所需数据");
        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        OrderInfo orderInfo = orderService.getOrderById(orderId);
        if (orderInfo == null) {
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }
        long goodsId = orderInfo.getGoodsId();
        GoodsVO goodsVO = goodsService.getGoodsVOByGoodsId(goodsId);
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setGoodsVO(goodsVO);
        orderDetailVO.setOrderInfo(orderInfo);
        return Result.success(orderDetailVO);
    }


}
