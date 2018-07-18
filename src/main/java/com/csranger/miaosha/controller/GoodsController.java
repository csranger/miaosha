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

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(value = "/goods")
public class GoodsController {

    private static final Logger logger = LoggerFactory.getLogger(MiaoshaUserService.class);


    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @Autowired
    private RedisService redisService;

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
    @RequestMapping(value = "/to_list")
    public String list(ModelMap modelMap, MiaoshaUser miaoshaUser) {
        // 每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息 这部分工作放到了 UserArgumentResolver 里面
        // 这里可以像 ModelMap 一样直接在 Controller 里添加 MiaoshaUser 对象
        modelMap.put("user", miaoshaUser);
        return "goods_list";
    }


}
