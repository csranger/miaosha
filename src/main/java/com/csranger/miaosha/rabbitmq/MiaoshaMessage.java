package com.csranger.miaosha.rabbitmq;

import com.csranger.miaosha.model.MiaoshaUser;

/**
 * rabbitmq 发送的消息对象，秒杀信息
 */
public class MiaoshaMessage {
    private MiaoshaUser miaoshaUser;
    private long goodsId;

    // setter getter
    public MiaoshaUser getMiaoshaUser() {
        return miaoshaUser;
    }

    public void setMiaoshaUser(MiaoshaUser miaoshaUser) {
        this.miaoshaUser = miaoshaUser;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(long goodsId) {
        this.goodsId = goodsId;
    }
}
