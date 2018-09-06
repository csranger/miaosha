package com.csranger.miaosha.access;

import com.alibaba.fastjson.JSON;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.redis.AccessKey;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * 接口拦截器 AccessInterceptor 来处理 @AccessLimit 注解
 */
@Service   // 拦截器交由容器管理
public class AccessInterceptor extends HandlerInterceptorAdapter {   // 继承拦截器基类


    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @Autowired
    private RedisService redisService;

    // 添加 @AccessLimit 注解的方法执行前进行拦截，进行一些处理，重写继承拦截器基类的 preHandle 方法
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {

            // 1. 获取用户对象：利用 request
            MiaoshaUser miaoshaUser = getUser(request, response);


            // 2. 获取注解 @AccessLimit 注解信息
            HandlerMethod hm = (HandlerMethod) handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);   // 获取方法上的 @AccessLimit 注解
            if (accessLimit == null) {      // 方法上不存在 @AccessLimit 注解则不需要进行任何限制
                return true;
            }
            // 如果方法上存在 @AccessLimit 注解，则获取注解的限制信息
            int seconds = accessLimit.seconds();
            int maxCounts = accessLimit.maxCounts();
            boolean needLogin = accessLimit.needLogin();

            // 3. 拦截器处理 @AccessLimit
            // 3.1 处理 needLogin
            String key = request.getRequestURI();
            if (needLogin) {  // 如果需要登录则判断 miaoshaUser 是否为空
                if (miaoshaUser == null) {
                    render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + miaoshaUser.getId();
            }
            // 3.2 处理 seconds maxCount
            AccessKey ak = AccessKey.withExpires(seconds);
            Integer count = redisService.get(ak, key, Integer.class);
            if (count == null) {
                redisService.set(ak, key, 1);
            } else if (count < maxCounts) {
                redisService.incr(ak, key);
            } else {
                render(response, CodeMsg.ACCESS_LIMIT_REACHED);
                return false;
            }

        }
        return super.preHandle(request, response, handler);
    }

    /**
     * 根据用户的 request 取出用户
     * request 如果 包含 cookie 中了包含了 token 则从 redis 中取出 MiaoshaUser 对象
     */
    private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
        // 每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息
        // 如果cookie里没有token(直接打开页面没有登陆的情况)，getByToken 方法从redis中去不成对象，返回 null
        String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);      // "token"值 被放在请求参数中获取
        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);  // "token"值 被放在 cookie 中获取
        if (StringUtils.isBlank(paramToken) && StringUtils.isBlank(cookieToken)) {
            return null;
        }
        String token = StringUtils.isBlank(paramToken) ? cookieToken : paramToken;
        return miaoshaUserService.getByToken(response, token);     // 从 redis 中利用 "token" 对应的值 UUID 取出 MiaoshaUser 对象

    }


    /**
     * 从用户请求中的 cookie 中取出 token 变量的值(UUID)
     * 从 request 中取出 "token" 对应的值 UUID
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 利用 response 输出 CodeMsg 信息
     */
    public void render(HttpServletResponse response, CodeMsg codeMsg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str = JSON.toJSONString(Result.error(codeMsg));
        out.write(str.getBytes("UTF-8"));
        out.flush();
        out.close();
    }



}
