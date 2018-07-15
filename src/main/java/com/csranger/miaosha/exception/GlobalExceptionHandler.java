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
 * 全局异常处理
 */
@ControllerAdvice         // 切面
@ResponseBody             // 此注解使得此类相当于 controller
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)     // 表明拦截说有异常
    public Result<String> exceptionHandler(HttpServletRequest request, Exception e) {   // 方法的参数和controller一样，HttpServletRequest和Exception均是系统传过来的
        // 1. 如果是 BindException 绑定异常
        if (e instanceof BindException) {
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
