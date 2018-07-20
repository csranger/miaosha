package com.csranger.miaosha.dao;

import com.csranger.miaosha.VO.GoodsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface GoodsDao {

    // 查询：商品信息+部分秒杀信息，为此创建 GoodsVO 结合 Goods + MiaoshaGoods
    @Select("select g.*, mg.miaosha_price, mg.stock_count, mg.start_time, mg.end_time from goods g right join miaosha_goods mg on g.id = mg.goods_id")
    List<GoodsVO> listGoodsVO();


    // 查询：根据商品 id 商品查询单个商品，用于商品详情页
    @Select("select g.*, mg.miaosha_price, mg.stock_count, mg.start_time, mg.end_time from goods g right join miaosha_goods mg on g.id = mg.goods_id where g.id = #{goodsId}")
    GoodsVO grtGoodsVOByGoodsId(@Param("goodsId") Long goodsId);


}
