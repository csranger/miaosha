package com.csranger.miaosha.VO;

import com.csranger.miaosha.model.MiaoshaUser;
import lombok.Data;

/**
 * goods_detail 静态页面所需要的数据
 * 类似于 OrderDetailVO
 */
@Data
public class GoodsDetailVO {

    private GoodsVO goodsVO;

    private int miaoshaStatus = 0;    // 代表秒杀状态 0 没开始 1 正在进行 2 结束
    private int remainSeconds = 0;    // 秒杀剩余时间 >0 没开始 0 正在进行 -1 结束

    private MiaoshaUser miaoshaUser;
}
