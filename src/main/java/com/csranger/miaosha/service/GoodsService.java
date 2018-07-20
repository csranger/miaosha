package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.dao.GoodsDao;
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


    // 查询某个秒杀商品：商品详情页需要
    public GoodsVO getGoodsVOByGoodsId(Long goodsId) {
        return goodsDao.grtGoodsVOByGoodsId(goodsId);
    }

}
