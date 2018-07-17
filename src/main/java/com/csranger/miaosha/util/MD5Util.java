package com.csranger.miaosha.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 这里的 md5 算法来自 commons-codec
 * 使用了两次md5加密 输入真实的密码 + 固定salt -> 表单加密密码 (+ 随机salt,存在数据库中) -> 数据库存储加密的密码
 */
public class MD5Util {

    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    private static final String salt = "1a2b3c4d";


    public static String encryPassword(String password, String randomSalt) {
        return fromPassToDBPass(inputPassToFormPass(password), randomSalt);
    }

    // 第一次md5：输入password通过 md5 加密到表单                     这里使用 固定salt
    public static String inputPassToFormPass(String password) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + password + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }


    // 第二次md5：将已加密的表单密码再次通过 md5 加密到数据库            这里使用随机 ransomSalt，也同时存入数据库
    public static String fromPassToDBPass(String formPassword, String randomSalt) {
        String str = "" + randomSalt.charAt(0) + randomSalt.charAt(2) + formPassword + randomSalt.charAt(5) + randomSalt.charAt(4);
        return md5(str);
    }

    public static void main(String[] args) {
        System.out.println(inputPassToFormPass("123456"));
        // 打印 d3b1294a61a07da9b49b6e22b2cbd7f9 这个结果和经过登陆经过 md5 js 加密后传递到后台的结果一样
        System.out.println(encryPassword("123456", "1a2b3c4d"));
        // 如果第二次加密的 randomSalt = 1a2b3c4d 则结果 b7797cce01b4b131b433b6acf4add449
    }

}
