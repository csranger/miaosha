package com.csranger.miaosha.controller;

import com.csranger.miaosha.model.User;
import com.csranger.miaosha.result.CodeMsg;
import com.csranger.miaosha.result.Result;
import com.csranger.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequestMapping(value = "/demo")
public class DemoCotroller {

    @Autowired
    private UserService userService;

    // 输出结果封装测试
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

}
