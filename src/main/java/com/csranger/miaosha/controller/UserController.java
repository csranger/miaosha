package com.csranger.miaosha.controller;

import com.csranger.miaosha.VO.GoodsVO;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.redis.RedisService;
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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping(value = "/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @Autowired
    private RedisService redisService;

    /**
     * 获取个人基本信息
     * 用于压测 /user/info 请求，单纯的获取 user 对象
     */
    @RequestMapping(value = "/info")
    @ResponseBody
    public Result<MiaoshaUser> list(Model model, MiaoshaUser miaoshaUser) {
        model.addAttribute("user", miaoshaUser);
        return Result.success(miaoshaUser);
    }

}
