package com.csranger.miaosha.controller;

import com.csranger.miaosha.VO.LoginVO;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.MiaoshaUserService;
import com.csranger.miaosha.service.UserService;
import com.csranger.miaosha.util.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping(value = "/login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private MiaoshaUserService miaoshaUserService;



    @RequestMapping(value = "/to_login")
    public String toLogin() {
        return "login";
    }



    // 测试时 使用12345678909 123456 登录测试 mysql 保存的两次 md5 密码是 b7797cce01b4b131b433b6acf4add449
    @RequestMapping(value = "/do_login")
    @ResponseBody
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVO loginVO) {
        // password 123456 时输出日志为 LoginVO{mobile='12345678909', password='d3b1294a61a07da9b49b6e22b2cbd7f9'}
        log.info(loginVO.toString());
        // 1. 参数校验：密码是否为空，手机号是否符合格式 之类的检查
        // 使用 JSR303 参数校验框架，参数校验产生异常会被异常处理器获取处理返回。所以如果这里还能执行说明无异常，登陆成功
//        String passInput = loginVO.getPassword();
//        String mobile = loginVO.getMobile();
//        if (StringUtils.isBlank(passInput)) {
//            return Result.error(CodeMsg.PASSWORD_EMPTY);
//        }
//        if (StringUtils.isBlank(mobile)) {
//            return Result.error(CodeMsg.MOBILE_EMPTY);
//        }
//        if (!ValidatorUtil.isMobile(mobile)) {
//            return Result.error(CodeMsg.MOBILE_ERROR);
//        }
        // 2. 登陆:判断手机号对应的账号是否存在于数据库，如果存在密码是否可以匹配上
        // 给这个用户生成一个 token 来标识这个用户, 将 token-user 缓存到 redis -> 写到 cookie 当中传递给客户端
//        miaoshaUserService.login(response, loginVO);
        String token = miaoshaUserService.login(response, loginVO);    // 登陆不成功会抛出异常从而处理

        return Result.success(token);
    }

}
