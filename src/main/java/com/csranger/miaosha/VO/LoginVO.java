package com.csranger.miaosha.VO;

import com.csranger.miaosha.validator.IsMobile;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * 登陆表单填写的数据
 * 登陆的表单传递给这个类的对象，登陆表单只有 mobile 和 密码，所以这里也只有两个属性
 */
@Data
public class LoginVO {

    @NotNull
    @IsMobile    // 自定义的校验器
    private String mobile;

    @NotNull
    @Length(min = 32)   // 这里密码最小32，是因为表单密码经md5会变成32位的数字
    private String password;

    // toString
    @Override
    public String toString() {
        return "LoginVO{" +
                "mobile='" + mobile + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
