package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.dao.GoodsDao;
import com.csranger.miaosha.model.MiaoshaGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {

    @Autowired
    private GoodsDao goodsDao;

    // 查询所有秒杀商品：商品列表页用到
    public List<GoodsVO> listGoodsVO() {
       return goodsDao.listGoodsVO();
    }


    // GoodsVO 商品列表页所需数据
    public GoodsVO getGoodsVOByGoodsId(Long goodsId) {
        return goodsDao.getGoodsVOByGoodsId(goodsId);
    }

    // 减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
    public void reduceStock(GoodsVO goods) {
        MiaoshaGoods mg = new MiaoshaGoods();
        mg.setGoodsId(goods.getId());     // miaoshaGoods 的商品id 是 goodsId
        goodsDao.reduceStock(mg);

    }

}
