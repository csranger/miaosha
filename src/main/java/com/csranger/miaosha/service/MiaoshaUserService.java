package com.csranger.miaosha.service;

import com.csranger.miaosha.VO.LoginVO;
import com.csranger.miaosha.dao.MiaoshaUserDao;
import com.csranger.miaosha.model.MiaoshaUser;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MiaoshaUserService {

    @Autowired
    private MiaoshaUserDao miaoshaUserDao;

    public MiaoshaUser getById(Long id) {
        return miaoshaUserDao.getById(id);
    }

    // 登陆账号就是根据 mobile 查询数据库是否有此用户的记录，如果有此id，则对比加密的 password 是否相同
    // 返回的 CodeMsg 代表登陆的消息：成功；失败 + 失败的消息msg
    public CodeMsg login(LoginVO loginVO) {
        if (loginVO == null) {
            return CodeMsg.SERVER_ERROR;
        }
        // 1.判断手机号是否存在于数据库中
        String password = loginVO.getPassword();
        String mobile = loginVO.getMobile();
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (user == null) {
            return  CodeMsg.MOBILE_NOT_EXOIST;
        }
        // 2. 如果手机号存在于数据库中
        String dbPass = user.getPassword();
        String dbSalt = user.getSlat();
        String pass = MD5Util.fromPassToDBPass(password, dbSalt);
        // 2.1 如果密码不匹配，登陆失败
        if (!dbPass.equals(pass)) {
            return CodeMsg.PASSWORD_ERROR;
        }
        // 2.2 密码匹配，登陆成功
        return CodeMsg.SUCCESS;

    }

}
