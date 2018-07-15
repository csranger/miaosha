package com.csranger.miaosha.VO;

/**
 * 登陆的表单传递给这个类的对象，登陆表单只有 mobile 和 密码，所以这里也只有两个属性
 */
public class LoginVO {

    private String mobile;

    private String password;

    // setter getter
    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // toString

    @Override
    public String toString() {
        return "LoginVO{" +
                "mobile='" + mobile + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
