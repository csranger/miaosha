package com.csranger.miaosha.model;

/**
 * 只有秒杀商品的订单叫做秒杀订单，因为秒杀活动可能只持续一段时间，就和正常情况下订单分开来
 */
public class MiaoshaOrder {

    private Long id;  // 自增
    private Long userId;
    private Long orderId;
    private Long goodsId;


    // setter getter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }
}
