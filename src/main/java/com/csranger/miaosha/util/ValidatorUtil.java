package com.csranger.miaosha.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 判断格式的工具类
 * 1. 判断手机格式是否正确
 * 2.
 * 3.
 */
public class ValidatorUtil {


    // 1开头后面跟着10个数字，则认为是正确的手机号
    private static final Pattern mobile_pattern = Pattern.compile("1\\d{10}");



    // 判断手机号格式是否正确的方法
    public static boolean isMobile(String src) {
        if (StringUtils.isBlank(src)) return false;
        Matcher m = mobile_pattern.matcher(src);
        return m.matches();
    }


    public static void main(String[] args) {
        System.out.println(isMobile("12345678909"));
    }

}
