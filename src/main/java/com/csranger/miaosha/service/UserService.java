package com.csranger.miaosha.service;

import com.csranger.miaosha.dao.UserDao;
import com.csranger.miaosha.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    // 一般提倡在自己的Service下引入自己的Dao(比如说在GoodsService引入GoodsDao)，其他的Dao通过引入对应的Service解决
    @Autowired
    private UserDao userDao;

    public User getUserById(int id) {
        return userDao.getUserById(id);
    }

    @Transactional
    public boolean tx() {
        // user1
        userDao.insert(4, "da");

        // user2
        userDao.insert(1, "shen");

        return true;
    }
}
