package com.csranger.miaosha.exception;

import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 全局异常处理:(1)处理 MiaoshaUser 里的 GlobalException (2)处理 LoginController 里的参数校验的 BindException
 * 1. @ControllerAdvice 控制器增强:@ControllerAdvice注解内部使用@ExceptionHandler、@InitBinder、@ModelAttribute注解的方法应用到所
 * 有的 @RequestMapping注解的方法,当使用@ExceptionHandler最有用
 * 2. @ControllerAdvice + @ExceptionHandler 进行全局的 Controller 层异常处理
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)     // 表明拦截说有异常
    public Result<String> exceptionHandler(HttpServletRequest request, Exception e) {   // 方法的参数和controller一样，HttpServletRequest和Exception均是系统传过来的

        // 如果是
        if (e instanceof GlobalException) {
            GlobalException ge = (GlobalException) e;
            return Result.error(ge.getCodeMsg());

        } else if (e instanceof BindException) {   // 如果是 BindException 绑定异常
            // 获取异常信息
            BindException be = (BindException) e;
            List<ObjectError> errors = be.getAllErrors();
            ObjectError error = errors.get(0);
            String msg = error.getDefaultMessage();
            // 将异常信息传到 CodeMsg
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        } else { // 2. 如果不是 BindException 绑定异常
            return Result.error(CodeMsg.SERVER_ERROR);
        }
    }
}
