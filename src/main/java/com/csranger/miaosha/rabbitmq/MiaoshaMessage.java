package com.csranger.miaosha.rabbitmq;

import com.csranger.miaosha.model.MiaoshaUser;
import lombok.Data;

/**
 * rabbitmq 发送的消息对象，秒杀信息
 */
@Data
public class MiaoshaMessage {
    private MiaoshaUser miaoshaUser;
    private long goodsId;
}
