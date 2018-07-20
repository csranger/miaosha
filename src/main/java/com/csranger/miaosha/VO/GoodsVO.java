package com.csranger.miaosha.VO;

import com.csranger.miaosha.model.Goods;

import java.util.Date;

/**
 * 代表秒杀商品，具有商品信息，也具有秒杀信息，理解成 Goods + MiaoshaGoods
 * 用于查询秒杀商品返回的对象
 */
public class GoodsVO extends Goods {

    private Double miaoshaPrice;
    private Integer stockCount;
    private Date startTime;
    private Date endTime;


    // setter getter
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
