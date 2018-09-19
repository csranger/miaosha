package com.csranger.miaosha.controller;

import com.csranger.miaosha.access.AccessLimit;
import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
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
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // goodsID -> true/false ： 商品 -> 此商品秒杀是否结束了
    // 目的：当秒杀商品库存没了，但是后来的秒杀请求还是不断的访问 redis，减少对redis 的访问
    private Map<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();


    // redis 预减库存+rabbitmq异步下单 优化秒杀功能
    // InitializingBean 接口抽象方法：容器启动时，发现实现此接口会回调这个抽象方法
    @Override
    public void afterPropertiesSet() throws Exception {
        // 1. 系统启动时就把商品库存数量加载到 redis:每个秒杀商品id是键，对应商品的库存是值
        // 同时利用 localOverMap 在本地标记每个商品有库存，可秒杀的
        List<GoodsVO> goodsVOList = goodsService.listGoodsVO();
        if (goodsVOList == null) {
            return;
        }
        for (GoodsVO goodsVO : goodsVOList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goodsVO.getId(), goodsVO.getStockCount());
            localOverMap.put(goodsVO.getId(), false);   // 商品标记此商品秒杀没有结束
        }

    }


    /**
     * BufferedImage ，代表着有数学表达式的验证码图片，通过 HttpServletResponse 的 outputStream 返回到客户端
     */
    @RequestMapping(value = "/verifyCodeImage", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCodeImage(MiaoshaUser miaoshaUser,
                                                    @RequestParam("goodsId") long goodsId,
                                                    HttpServletResponse response) {
        logger.info("正在进行验证码验证");
        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        // 生成验证码图片：将一个数学表达式写在验证码图片上，同时将计算结果缓存到 redis，返回这个图片
        // miaoshaUser, goodsId是用来作为验证码答案存在 redis 的键
        // 注意这里使用的是 response 的 outputStream 将这个图片返回到客户端的，所以 return null
        try {
            BufferedImage image = miaoshaService.createVerifyCodeImage(miaoshaUser, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            return null;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }

    /**
     * 秒杀地址隐藏：点击秒杀按钮，先对比验证码，验证码正确的话再获取秒杀地址
     * 生成一个随机数，返回给客户端，客户端立即请求/miaosha/{psth}/do_miaosha，才可进行秒杀：这样就隐藏了秒杀路径
     */
    @AccessLimit(seconds = 5, maxCounts = 5, needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(MiaoshaUser miaoshaUser,
                                         @RequestParam("goodsId") long goodsId,
                                         @RequestParam("verifyCode") int verifyCode) {
        logger.info("正在获取秒杀地址");
        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 验证码是否正确: miaoshaUser, goodsId 是为了从 redis 中取出答案和 verifyCode 进行对比
        boolean check = miaoshaService.checkVerifyCode(miaoshaUser, goodsId, verifyCode);
        if (!check) {
            return Result.error(CodeMsg.VERIFY_CODE_ERROE);
        }

        // 生成一个随机数作为秒杀请求地址，返回给客户端，客户端才知道秒杀地址请求秒杀 + 将这个随机值暂时缓存在 redis，以确认秒杀地址是否正确
        String path = miaoshaService.createPath(miaoshaUser, goodsId);

        return Result.success(path);
    }

    /**
     * 进行秒杀
     */
    @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(MiaoshaUser miaoshaUser,
                                   @RequestParam("goodsId") long goodsId,
                                   @PathVariable("path") String path) {
        logger.info("用户 " + miaoshaUser.getId() + " 正在秒杀，秒杀商品的id是 " + goodsId);

        if (miaoshaUser == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        // 验证{path} 键是 miaoshaUser.getId() + "_" + goodsId
        boolean check = miaoshaService.checkPath(miaoshaUser, goodsId, path);
        if (!check) {   // 验证失败，返回请求非法
            return Result.error(CodeMsg.REQUEST_PATH_ILLEGAL);
        }

        // 本地缓存记录商品是否秒杀结束：减少秒杀库存没了后之后的用户依然发起秒杀请求对redis的访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.MIAOSHA_OVER);
        }

        // 2. 收到请求，redis 预减库存，库存不足，直接返回，否则继续；返回的是剩下的库存
        // 可能会出现单个用户同时发起多个请求，减库存了，但是真正生成订单的只有1个，这可以通过验证码防止，另外卖超是不允许的，卖不完是允许的
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
        if (stock < 0) {
            localOverMap.put(goodsId, true);
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
