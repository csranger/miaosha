package com.csranger.miaosha.model;

import java.util.Date;

/**
 * 秒杀商品(goodsId)及其秒杀相关信息：维护Goods的稳定
 */
public class MiaoshaGoods {

    private Long id;
    private Long goodsId;
    // 秒杀相关信息:秒杀价，存库，起始与结束时间
    private Double miaoshaPrice;
    private Integer stockCount;
    private Date startTime;
    private Date endTime;


    // setter getter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Double getMiaoshaPrice() {
        return miaoshaPrice;
    }

    public void setMiaoshaPrice(Double miaoshaPrice) {
        this.miaoshaPrice = miaoshaPrice;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
