package com.csranger.miaosha.controller;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaOrder;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.model.OrderInfo;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.service.GoodsService;
import com.csranger.miaosha.service.MiaoshaService;
import com.csranger.miaosha.service.MiaoshaUserService;
import com.csranger.miaosha.service.OrderService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping(value = "/goods")
public class GoodsController {

    private static final Logger logger = LoggerFactory.getLogger(GoodsController.class);


    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MiaoshaService miaoshaService;


//    ！！！！每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息，这就很麻烦！！！！
//    /**
//     * 服务器给用户设定了 cookie 之后，客户端在随后的访问当中都带有这个值，使用 @CookieValue 注解获取到这个值
//     *
//     * 有的时候会有手机客户端将 cookie 中 token 放在参数里面传，为了兼容这种情况，加上 @RequestParam("token")
//     */
//    @RequestMapping(value = "/to_list")
//    public String list(HttpServletResponse response, ModelMap modelMap,
//                         @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String cookieToken,
//                         @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String paramToken
//    ) {
//        if (StringUtils.isBlank(cookieToken) && StringUtils.isBlank(paramToken)) {
//            return "login";
//        }
//        // 获取 cookie 中 token
//        String token = StringUtils.isBlank(paramToken) ? cookieToken : paramToken;
//        // 利用 token 从 redis 拿出 MiaoshaUser 信息
//        MiaoshaUser miaoshaUser = miaoshaUserService.getByToken(response, token);
//
//        modelMap.put("user", miaoshaUser);
//        return "goods_list";
//    }

    //    ！！！！每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息，这就很麻烦！！！！
    @RequestMapping(value = "/to_list2")
    public String list(ModelMap modelMap, MiaoshaUser miaoshaUser) {
        // 每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息 这部分工作放到了 UserArgumentResolver 里面
        // 这里可以像 ModelMap 一样直接在 Controller 里添加 MiaoshaUser 对象
        modelMap.put("user", miaoshaUser);
        return "goods_list2";
    }


    // 查询秒杀商品列表
    @RequestMapping(value = "/to_list")
    public String list(Model model, MiaoshaUser miaoshaUser) {
        model.addAttribute("user", miaoshaUser);

        // 查询商品列表
        List<GoodsVO> goodsList = goodsService.listGoodsVO();

        model.addAttribute("goodsList", goodsList);
        return "goods_list";
    }

    // 秒杀商品商品详情页
    @RequestMapping(value = "/to_detail/{goodsId}")
    public String detail(Model model, MiaoshaUser miaoshaUser, @PathVariable("goodsId") long goodsId) {
        model.addAttribute("user", miaoshaUser);

        // 查询某个id的商品(商品详情页点击详情查询某个商品具体信息)
        GoodsVO goods = goodsService.getGoodsVOByGoodsId(goodsId);
        logger.info("startTime：" + goods.getStartTime() + "  endTime: " + goods.getEndTime());
        model.addAttribute("goods", goods);

        // 该商品的 秒杀状态+秒杀剩余时间
        int miaoshaStatus = 0;    // 代表秒杀状态 0 没开始 1 正在进行 2 结束
        int remainSeconds = 0;    // 秒杀剩余时间 >0 没开始 0 正在进行 -1 结束
        Long start = goods.getStartTime().getTime();    // Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
        Long end = goods.getEndTime().getTime();
        long now2 = new Date().getTime();
        long now = System.currentTimeMillis();
        logger.info("now == now2 ?" + (now == now2));
        if (now < start) {         // 秒杀没开始
            miaoshaStatus = 0;
            remainSeconds = (int) ((start - now) / 1000);
        } else if (now > end) {     // 秒杀结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {                    // 秒杀正在进行
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        logger.info("" + now);
        logger.info("miaoshaStatus: " + miaoshaStatus + "\t" + "remainSeconds: " + remainSeconds);
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);


        return "goods_detail";
    }



}
