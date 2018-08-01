package com.csranger.miaosha.VO;

import com.csranger.miaosha.model.OrderInfo;

/**
 * order_detail 静态页面所需要的数据
 * 类似于 GoodsDetailVO
 */
public class OrderDetailVO {

    private GoodsVO goodsVO;

    private OrderInfo orderInfo;

    // setter getter
    public GoodsVO getGoodsVO() {
        return goodsVO;
    }

    public void setGoodsVO(GoodsVO goodsVO) {
        this.goodsVO = goodsVO;
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }
}
