package com.csranger.miaosha.validator;

import com.csranger.miaosha.util.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @IsMobile 注解的校验器
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {    // IsMobile 代表注解 String 代表注解修饰字段的类型

    private boolean required = false;


    // 参数 String s 就是 注解需要校验的字段，即 LoginVO 类的 private String mobile;
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        // 1. 如果需要校验的字段是必须的，则正常校验校验：判断手机号是否格式正确
        if (required) {
            return ValidatorUtil.isMobile(s);
        } else {    // 2. 如果该字段是必须的，先判断是否为空
            if (StringUtils.isBlank(s)) {    // 2.1 如果该字段为空，说明没填写该字段
                return true;
            } else {                          // 2.2 如果该字段不为空，则正常校验：判断手机号是否格式正确
                return ValidatorUtil.isMobile(s);
            }
        }
    }


    // 初始化方法可以拿到 @IsMobile 注解
    @Override
    public void initialize(IsMobile constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }
}
