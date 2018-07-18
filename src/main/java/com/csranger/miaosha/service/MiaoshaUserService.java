package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.LoginVO;
import com.csranger.miaosha.dao.MiaoshaUserDao;
import com.csranger.miaosha.exception.GlobalException;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.redis.MiaoshaUserKey;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.util.MD5Util;
import com.csranger.miaosha.util.UUIDUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class MiaoshaUserService {

    private static final Logger logger = LoggerFactory.getLogger(MiaoshaUserService.class);

    public static final String COOKIE_NAME_TOKEN = "token";   // 指的是放入 cookie 中的变量的名字，就是字符串 "token"

    @Autowired
    private MiaoshaUserDao miaoshaUserDao;

    @Autowired
    private RedisService redisService;

    public MiaoshaUser getById(Long id) {
        return miaoshaUserDao.getById(id);
    }

    // 登陆账号就是根据 mobile 查询数据库是否有此用户的记录，如果有此id，则对比加密的 password 是否相同
    // 返回的 CodeMsg 代表登陆的消息：成功；失败 + 失败的消息msg
    // CodeMsg 作为 service层不合适，改成 boolean，遇到异常不再返回 CodeMsg 而是 GlobalException，将 CodeMsg 作为异常信息
    public boolean login(HttpServletResponse response, LoginVO loginVO) {
        if (loginVO == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        // 1.判断手机号是否存在于数据库中
        String password = loginVO.getPassword();
        String mobile = loginVO.getMobile();
        MiaoshaUser miaoshaUser = getById(Long.parseLong(mobile));     // 用户信息
        if (miaoshaUser == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXOIST);
        }
        // 2. 如果手机号存在于数据库中,进行密码匹配
        String dbPass = miaoshaUser.getPassword();
        String dbSalt = miaoshaUser.getSalt();
        String pass = MD5Util.fromPassToDBPass(password, dbSalt);
        // 2.1 如果密码不匹配，登陆失败
        if (!dbPass.equals(pass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        // 2.2 密码匹配，意味着登陆成功

        // 生成 cookie/实现 session 功能：登陆成功之后，给这个用户生成一个类似于 sessionId 的变量 token 来标识这个用户 -> 写到
        // cookie 当中传递给客户端 -> [ 客户端在随后的访问当中都在 cookie 上传这个 token -> 服务端拿到这个 token 之后
        // 就根据这个 token 取到用户对应的 sesession 信息 ] 后面步骤浏览器来做
        String token = UUIDUtil.uuid();     // 用户登录后将此用户 token 记住不需要没打开一个页面都生成一个新的 token
        addCookie(response, token,  miaoshaUser);
        logger.info("生成 token 放入 cookie，写到 response 中发送给客户端");

        return true;

    }


    /**
     * 登陆成功之后，给这个用户生成一个 token 来标识这个用户, 将 token-user 缓存到 redis -> 写到 cookie 当中传递给客户端
     */
    private void addCookie(HttpServletResponse response, String token, MiaoshaUser miaoshaUser) {
        // 1 将 token-user 再次写到 redis 缓存中，前缀已经设定了存储到 redis 中的过期时间：相当于刷新了 过期时间
        redisService.set(MiaoshaUserKey.token, token, miaoshaUser);

        // 2 token 放入 cookie
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());      // cookie 的有效期设置为 MiaoshaUserKey.token.expireSeconds() 即让其等于 redis 保存有效期期
        cookie.setPath("/");

        // 3 写到 response ，所以参数里加上 HttpServletResponse,service中方法不可直接加上HttpServletResponse；需要在Controller中加上，类似于 ModelMap map
        response.addCookie(cookie);
    }


    /**
     * getByToken 方法被调用是在用户已经登陆，用户的请求 cookie 中含有 token 时，在打开新的页面，为了不再需要登陆确认 user 信息直接从 redis 中获取 user 信息，这就
     * 需要更新 redis 中的过期时间和 cookie 中token 值：意思是从现在开始从新算过期时间
     */
    public MiaoshaUser getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        MiaoshaUser miaoshaUser = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
        // 从现在开始算过期时间
        if (miaoshaUser != null) {
            addCookie(response, token, miaoshaUser);
        }
        return miaoshaUser;

    }
}