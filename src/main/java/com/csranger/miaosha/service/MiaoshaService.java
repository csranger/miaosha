package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import com.csranger.miaosha.redis.MiaoshaKey;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.util.MD5Util;
import com.csranger.miaosha.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 将执行秒杀操作放入，进行事务管理
 * (1)减库存 -> (2)下订单 -> (3)数据库插入秒杀订单
 */
@Service
public class MiaoshaService {

    private static char[] ops = new char[]{'+', '-', '*'};  // 代表 加减乘 运算符


    /**
     * 这里引入 GoodsService 是为了使用 GoodsDao，因为一般提倡在自己的Service下引入自己的Dao(比如说在GoodsService引入GoodsDao)，
     * 所以这里引入 GoodsService 解决
     */
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisService redisService;

    // #1.1
    // 进行秒杀：(1)减库存 -> (2)数据库插入生成的秒杀订单与订单
    @Transactional
    public OrderInfo miaosha(MiaoshaUser user, GoodsVO goods) {
        // (1)减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
        // 虽然 MiaoshaController 里已经在 MiaoshaController 里检查了是否有库存，但可能发生多个请求同时检查库存，例如库存为2，同时有10个请求，
        // 这时有库存，但实际数据库操作就会减库存失败8次，因此这8次不可以生成订单，因为库存没了
        boolean success = goodsService.reduceStock(goods);
        // (2)数据库插入生成的秒杀订单与订单:即在 order_info 和 miaosha_order 表中插入一条记录
        // miaosha_order 是 order_info 子集，只包含参加秒杀活动的商品订单
        if (success) {
            return orderService.createOrder(user, goods);
        } else {
            // 库存没了，标记该商品秒杀卖完了：在redis中插入 goodsId -> true(商品卖完)
            // 重要，当客户端请求秒杀结果时，用于区分是在排队还是秒杀失败
            setGoodsOver(goods.getId());
            return null;    // throw new GlobalException(CodeMsg.MIAOSHA_OVER)
        }
    }

    /**
     * #2.1
     * orderId : 秒杀成功，数据库中可以查到订单，返回订单id即可
     * -1      : 秒杀失败，意味着商品秒杀卖完了
     * 0       : 排队中，客户端继续轮询，再次查询秒杀结果
     * 注意点：秒杀失败和排队中两种情况下均查不到订单，如何区分开来？
     * 查不到订单情况下，如果该商品没有库存，说明秒杀失败，如果该商品有库存，说明还在队列中
     */
    public long getMiaoshaResult(long userId, long goodsId) {
        // 从数据库里查是否生成了订单
        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
        if (miaoshaOrder != null) {   // 生成了订单 秒杀成功
            return miaoshaOrder.getOrderId();
        } else {
            // 数据库查不到订单原因 1. 秒杀失败 2. 在 rabbitmq 队列中，排队中，还没执行到   如何区分？
            // ajax 发起里查询秒杀结果的请求，说明秒杀请求已经提交到rabbitmq队列，如果还有此商品的库存，则应该是在排队中，反之秒杀失败
            boolean isOver = getGoodsOver(goodsId);     // true 表示没库存了即秒杀失败
            if (isOver) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    // #1.2
    // 在 redis 中标记此商品卖完 即 goodsId -> true
    private void setGoodsOver(long goodsId) {
        redisService.set(MiaoshaKey.isGoodsOver, "" + goodsId, true);
    }

    // #2.2
    // 查看 redis 中是否标记此商品卖完
    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(MiaoshaKey.isGoodsOver, "" + goodsId);
    }

    // #3
    // 生成一个随机数作为秒杀请求地址，返回给客户端，客户端才知道秒杀地址请求秒杀 + 将这个随机值暂时缓存在 redis，以确认秒杀地址是否正确
    public String createPath(MiaoshaUser miaoshaUser, long goodsId) {
        String path = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisService.set(MiaoshaKey.getMiaoshaPath, "" + miaoshaUser.getId() + "_" + goodsId, path);
        return path;
    }

    // #4
    // 验证秒杀路径是否正确，与redis暂时缓存的随机值进行对比
    public boolean checkPath(MiaoshaUser miaoshaUser, long goodsId, String path) {
        if (path == null || goodsId <= 0) {
            return false;
        }
        String pathInRedis = redisService.get(MiaoshaKey.getMiaoshaPath, "" + miaoshaUser.getId() + "_" + goodsId, String.class);
        return path.equals(pathInRedis);
    }


    // #5.1 生成验证码图片：将一个数学表达式写在验证码图片上，同时将计算结果缓存到 redis，返回这个图片
    public BufferedImage createVerifyCodeImage(MiaoshaUser miaoshaUser, long goodsId) {
        if (miaoshaUser == null || goodsId <= 0) {
            return null;
        }
        int width = 90;
        int height = 32;
        // 创建 BufferedImage 对象：内存里的图像
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();    // Graphics 看成画笔
        // 设定背景颜色：以 0xDCDCDC 颜色填充
        graphics.setColor(new Color(0xDCDCDC));
        graphics.fillRect(0, 0, width, height);
        // 以黑色画个矩形框
        graphics.setColor(Color.black);
        graphics.drawRect(0, 0, width - 1, height - 1);
        // 50 个随机干扰点
        Random rdm = new Random();
        for (int i = 0; i < 50; i++) {
            int x = rdm.nextInt(width);
            int y = rdm.nextInt(height);
            graphics.drawOval(x, y, 0, 0);
        }
        // 生成 数学公式 的字符串
        String verifyCode = createVerifyCode(rdm);
        graphics.setColor(new Color(0, 100, 0));                 // 画笔颜色
        graphics.setFont(new Font("Candara", Font.BOLD, 24));  // 画笔字体
        graphics.drawString(verifyCode, 8, 24);
        graphics.dispose();

        // 数学公式 的字符串的计算结果缓存到 redis
        int answer = calc(verifyCode);
        redisService.set(MiaoshaKey.getMiaoshaVerifyCode, miaoshaUser.getId() + "_" + goodsId, answer);

        // 输出图片
        return image;
    }

    // #5.2 生成 数学公式 的字符串
    private String createVerifyCode(Random rdm) {
        int number1 = rdm.nextInt(10);    // [0, 10) 之间的随机数
        int number2 = rdm.nextInt(10);
        int number3 = rdm.nextInt(10);
        char ops1 = ops[rdm.nextInt(3)];   // 没有除法是为了防止除以0异常，简化代码
        char ops2 = ops[rdm.nextInt(3)];
        return "" + number1 + ops1 + number2 + ops2 + number3;
    }

    // #5.3 计算数学表达式字符串的结果
    public int calc(String exp) {
        try{
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return (Integer) engine.eval(exp);
        } catch (ScriptException e){
            e.printStackTrace();
            return 0;
        }
    }

    public static void main(String[] args) {
        // 测试下 calc 函数
        String exp = new MiaoshaService().createVerifyCode(new Random());
        System.out.println(exp);
        int ans = new MiaoshaService().calc(exp);
        System.out.println(ans);
    }

    // #6 匹配验证码，并删掉缓存在 redis 中的值
    public boolean checkVerifyCode(MiaoshaUser miaoshaUser, long goodsId, int verifyCode) {
        if (miaoshaUser == null || goodsId <= 0) {
            return false;
        }
        Integer verifyCodeInRedis = redisService.get(MiaoshaKey.getMiaoshaVerifyCode, miaoshaUser.getId() + "_" + goodsId, Integer.class);
        if (verifyCodeInRedis == null || verifyCodeInRedis - verifyCode != 0) {
            return false;
        }
        redisService.delete(MiaoshaKey.getMiaoshaVerifyCode, miaoshaUser.getId() + "_" + goodsId);
        return true;
    }
}
