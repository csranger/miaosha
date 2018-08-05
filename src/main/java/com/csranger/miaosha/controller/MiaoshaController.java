package com.csranger.miaosha.controller;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import com.csranger.miaosha.rabbitmq.MQSender;
import com.csranger.miaosha.rabbitmq.MiaoshaMessage;
import com.csranger.miaosha.redis.GoodsKey;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.GoodsService;
import com.csranger.miaosha.service.MiaoshaService;
import com.csranger.miaosha.service.MiaoshaUserService;
import com.csranger.miaosha.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(value = "/miaosha")
public class MiaoshaController implements InitializingBean {

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

    @Autowired
    private MQSender mqSender;


// 执行秒杀功能：点击秒杀按钮，发送一个含有 商品id 的表单到 /goods/do_miaosha 页面
//    @RequestMapping(value = "do_miaosha", method = RequestMethod.POST)
//    @ResponseBody
//    public Result<OrderInfo> miaosha(MiaoshaUser miaoshaUser, Model model, @RequestParam("goodsId") long goodsId) {
//        logger.info("用户 " + miaoshaUser.getId() + " 正在秒杀，秒杀商品的id是 " + goodsId);
////        model.addAttribute("user", miaoshaUser);
//        // 0. 限制条件，如果用户没登陆，反复登陆页面
//        if (miaoshaUser == null) {
//            return Result.error(CodeMsg.SESSION_ERROR);
//        }
//        // 1. 判断是否有库存：如果此 goodsId 对应商品没有库存
//        GoodsVO goods = goodsService.getGoodsVOByGoodsId(goodsId);                                                // GoodsVO good
//        int stockCount = goods.getStockCount();
//        if (stockCount <= 0) {
//            return Result.error(CodeMsg.MIAOSHA_OVER);
//        }
//        // 2. 判断是否已经秒杀：如果已经秒杀过了
//        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);   // MiaoshaUser user
//        if (miaoshaOrder != null) {
//            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
//            return Result.error(CodeMsg.REPEATE_MIAOSHA);
//        }
//        // 3. 进行秒杀：(1)减库存 -> (2)数据库插入生成的秒杀订单与订单  这两个步骤需要 事务管理
//        // 因此可以将这几步放入 MiaoshaService 进行操作，秒杀成功进入订单详情页
//        // 进行秒杀操作只需 MiaoshaUser user, GoodsVO goods
//        OrderInfo orderInfo = miaoshaService.miaosha(miaoshaUser, goods);
////        model.addAttribute("orderInfo", orderInfo);
////        model.addAttribute("goods", goods);
////        return "order_detail";
//        return Result.success(orderInfo);
//    }


    // redis 预减库存+rabbitmq异步下单 优化秒杀功能
    // InitializingBean 接口抽象方法：容器启动时，发现实现此接口会回调这个抽象方法
    @Override
    public void afterPropertiesSet() throws Exception {
        // 1. 系统启动时就把商品库存数量加载到 redis:每个秒杀商品id是键，对应商品的库存是值
        List<GoodsVO> goodsVOList = goodsService.listGoodsVO();
        if (goodsVOList == null) {
            return;
        }
        for (GoodsVO goodsVO : goodsVOList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goodsVO.getId(), goodsVO.getStockCount());
        }

    }

    @RequestMapping(value = "/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(MiaoshaUser miaoshaUser, @RequestParam("goodsId") long goodsId) {
        logger.info("用户 " + miaoshaUser.getId() + " 正在秒杀，秒杀商品的id是 " + goodsId);

        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        // 2. 收到请求，redis 预减库存，库存不足，直接返回，否则继续；返回的是剩下的库存
        // 可能会出现单个用户同时发起多个请求，减库存了，但是真正生成订单的只有1个，这可以通过验证码防止，另外卖超是不允许的，卖不完是允许的
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
        if (stock < 0) {
            return Result.error(CodeMsg.MIAOSHA_OVER);
        }
        // 判断是否秒杀过了：根据 userId 和 goodsId 查询是否有订单存在
        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
        if (miaoshaOrder != null) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        // 3. 秒杀请求压入 rabbitmq 队列，立即返回排队中(无阻塞)
        MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
        miaoshaMessage.setGoodsId(goodsId);
        miaoshaMessage.setMiaoshaUser(miaoshaUser);
        mqSender.sendMiaoshaMessage(miaoshaMessage);
        return Result.success(0);   // 秒杀请求压入 rabbitmq 队列，立即返回，无阻塞
    }


    /**
     * 查询秒杀结果：如果秒杀成功，数据库里有订单记录，则返回订单id
     * orderId : 秒杀成功
     * -1      : 秒杀失败
     * 0       : 排队中，客户端继续轮询
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> result(MiaoshaUser miaoshaUser, @RequestParam("goodsId") long goodsId) {
        logger.info("正在获取秒杀结果订单");
        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        // 查询是否生成里订单:如果秒杀成功，数据库里有订单记录，则返回订单id
        long result = miaoshaService.getMiaoshaResult(miaoshaUser.getId(), goodsId);

        return Result.success(result);
    }
}
