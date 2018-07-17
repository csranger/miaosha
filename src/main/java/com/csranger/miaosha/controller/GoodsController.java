package com.csranger.miaosha.controller;

import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/goods")
public class GoodsController {

    private static final Logger logger = LoggerFactory.getLogger(MiaoshaUserService.class);


    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @Autowired
    private RedisService redisService;


    /**
     * 服务器给用户设定了 cookie 之后，客户端在随后的访问当中都带有这个值，使用 @CookieValue 注解获取到这个值
     *
     * @CookieValue 中的 value 指的是放入 cookie 中键的字符串 "token"，它的值是 UUID，代表发起请求的机器，UUID是这个机器的唯一标识
     * <p>
     * 有的时候会有手机客户端将 cookie 中 token 放在参数里面传，为了兼容这种情况，加上 @RequestParam("token")
     */
    @RequestMapping(value = "/to_list")
    public String toList(ModelMap modelMap,
                         @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String cookieToken,
                         @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String paramToken) {
        if (StringUtils.isBlank(cookieToken) && StringUtils.isBlank(paramToken)) {
            return "login";
        }
        // 获取 cookie 中 token
        String token = StringUtils.isBlank(paramToken) ? cookieToken : paramToken;
        // 利用 token 从 redis 拿出 MiaoshaUser 信息
        MiaoshaUser miaoshaUser = miaoshaUserService.getByToken(token);

        modelMap.put("user", miaoshaUser);
        return "goods_list";
    }
}
