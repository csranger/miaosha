package com.csranger.miaosha.model;

import lombok.Data;

import java.util.Date;

/**
 * 秒杀商品(goodsId)及其秒杀相关信息：维护Goods的稳定
 */
@Data
public class MiaoshaGoods {

    private Long id;
    private Long goodsId;
    // 秒杀相关信息:秒杀价，存库，起始与结束时间
    private Double miaoshaPrice;
    private Integer stockCount;
    private Date startTime;
    private Date endTime;
}
