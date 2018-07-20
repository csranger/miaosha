package com.csranger.miaosha.model;

/**
 * 代表商品，不包含秒杀信息
 */
public class Goods {

    // 这里商品 id 使用了自增键，简化项目，工程实际不会，因为很容易被人遍历。使用 snowflake 算法
    private long id;
    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private Double goodsPrice;
    private Integer goodsStock;


    // setter getter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsTitle() {
        return goodsTitle;
    }

    public void setGoodsTitle(String goodsTitle) {
        this.goodsTitle = goodsTitle;
    }

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }

    public Double getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(Double goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public Integer getGoodsStock() {
        return goodsStock;
    }

    public void setGoodsStock(Integer goodsStock) {
        this.goodsStock = goodsStock;
    }
}
