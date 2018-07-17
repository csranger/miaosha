package com.csranger.miaosha.controller;

import com.csranger.miaosha.model.User;
import com.csranger.miaosha.redis.RedisService;
import com.csranger.miaosha.redis.UserKey;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * User UserDao UserService DemoController 用于测试集成 Thymeleaf mybatis redis
 */
@Controller
@RequestMapping(value = "/demo")
public class DemoCotroller {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisService redisService;

    // 输出结果 Result 封装测试
    @RequestMapping(value = "/hello")
    @ResponseBody
    public Result<String> hello() {
        return Result.success("Hello, world!");
    }

    @RequestMapping(value = "/helloError")
    @ResponseBody
    public Result<String> helloError() {
        return Result.error(CodeMsg.SERVER_ERROR);
        // return new Result(500102, "XXX");
    }

    // 集成 thymeleaf 测试
    @RequestMapping(value = "/thymeleaf/{name}")
    public String thymeleaf(@PathVariable("name") String name) {
        return "hello";    // 加上前缀与后缀就是页面位置(application.properties中设置了)
    }


    // 集成 Mybatis 测试
    @RequestMapping("/user/{id}")
    @ResponseBody
    public Result<User> getUserById(@PathVariable("id") int id) {
        User user = userService.getUserById(id);
        // user是结果，封装结果，使用Result，不仅包括数据user，也包括带好吃
        return Result.success(user);
    }

    // 事务测试
    @RequestMapping("/tx")
    @ResponseBody
    public Result<Boolean> tx() {
        return Result.success(userService.tx());
    }

//    // 集成 redis 测试
//    @RequestMapping(value = "/redis/get")
//    @ResponseBody
//    public  Result<Long> redisGet() {
//        Long v1 = redisService.get("k1", Long.class);
//        return Result.success(v1);
//    }
//
//    @RequestMapping(value = "/redis/set")
//    @ResponseBody
//    public  Result<String> redisSet() {
//        // 键是"k2"，始终是String，值可以是任意类型T，为了存在redis中，将T转化成String，再存
//        Boolean ret = redisService.set("k2", "Hello, world!");
//        // 键是"k2"，从redis中取出值，并同时告诉get方法转换成什么类型，即存储前值的类型
//        String v2 = redisService.get("k2", String.class);
//        // 将获得的结果通过 Result 封装使用静态方法输出结果
//        return Result.success(v2);
//    }

    // redis + 结合前缀
    @GetMapping(value = "/redis/setuser")
    @ResponseBody
    public Result<Boolean> redisSet() {
        // 创建一个User对象
        User user = new User();
        user.setId(1);
        user.setName("1111");
        // 将 键-值 存入 redis 中，参数：前缀，键，值
        redisService.set(UserKey.getById, "" + 1, user);
        return Result.success(true);
    }



}
