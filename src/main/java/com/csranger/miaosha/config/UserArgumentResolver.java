package com.csranger.miaosha.config;

import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息，这就很麻烦
 */
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private MiaoshaUserService miaoshaUserService;

    // resolveArgument 方法返回 MiaoshaUser 对象
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> clazz = parameter.getParameterType();    // 获取参数类型
        return clazz == MiaoshaUser.class;
    }


    // 参数类型为 MiaoshaUser.class 的才会做以下处理
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

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
}
