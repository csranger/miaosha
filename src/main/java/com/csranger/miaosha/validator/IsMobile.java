package com.csranger.miaosha.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import java.lang.annotation.*;

/**
 * 仿照 @NotNull 自定义一个验证器：验证该属性是手机号
 */

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {IsMobileValidator.class})     // 遇到 @IsMobile 注解时会调用 IsMobileValidator 校验器来进行校验
public @interface IsMobile {

    // 如果验证不通过，提示此信息
    String message() default "手机号码格式错误";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 个人添加的代表这个需要校验的 字段 是不是必须的，默认是必须的
    // 这些成员可以通过 @IsMobile(required = false) 设置值
    boolean required() default true;

}
