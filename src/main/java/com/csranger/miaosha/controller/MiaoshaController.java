package com.csranger.miaosha.controller;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.GoodsService;
import com.csranger.miaosha.service.MiaoshaService;
import com.csranger.miaosha.service.MiaoshaUserService;
import com.csranger.miaosha.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/miaosha")
public class MiaoshaController {

    private static final Logger logger = LoggerFactory.getLogger(MiaoshaController.class);

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MiaoshaService miaoshaService;



    // 执行秒杀功能：点击秒杀按钮，发送一个含有 商品id 的表单到 /goods/do_miaosha 页面
    @RequestMapping(value = "do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<OrderInfo> miaosha(MiaoshaUser miaoshaUser, Model model, @RequestParam("goodsId") long goodsId) {
        logger.info("用户 " + miaoshaUser.getId() + " 正在秒杀，秒杀商品的id是 " + goodsId);
//        model.addAttribute("user", miaoshaUser);
        // 1. 限制条件，如果用户没登陆，反复登陆页面
        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        // 2. 判断是否有库存：如果此 goodsId 对应商品没有库存
        GoodsVO goods = goodsService.getGoodsVOByGoodsId(goodsId);                                                // GoodsVO good
        int stockCount = goods.getStockCount();
        if (stockCount <= 0) {
            return Result.error(CodeMsg.MIAOSHA_OVER);
        }
        // 3. 判断是否已经秒杀：如果已经秒杀过了
        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);   // MiaoshaUser user
        if (miaoshaOrder != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        // 4. 进行秒杀：(1)减库存 -> (2)数据库插入生成的秒杀订单与订单  这两个步骤需要 事务管理
        // 因此可以将这几步放入 MiaoshaService 进行操作，秒杀成功进入订单详情页
        // 进行秒杀操作只需 MiaoshaUser user, GoodsVO goods
        OrderInfo orderInfo = miaoshaService.miaosha(miaoshaUser, goods);
//        model.addAttribute("orderInfo", orderInfo);
//        model.addAttribute("goods", goods);
//        return "order_detail";
        return Result.success(orderInfo);
    }
}
