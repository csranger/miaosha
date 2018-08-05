package com.csranger.miaosha.controller;

import com.csranger.miaosha.rabbitmq.MQSender;
import com.csranger.miaosha.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 用于测试
 */
@Controller
@RequestMapping(value = "/demo")
public class SampleController {

    @Autowired
    private MQSender mqSender;


    /**
     * 测试 rabbitmq 的 4 中交换机模式
     */

    // 1. direct
    @RequestMapping(value = "/mq/direct")
    @ResponseBody
    public Result<String> mq() {
        mqSender.sendDirect("Hello, mq - direct!");
        return Result.success("Hello, mq - direct!");
    }


    // 2. topic
    @RequestMapping(value = "/mq/topic")
    @ResponseBody
    public Result<String> topic() {
        mqSender.sendTopic("Hello, mq - topic!");
        return Result.success("Hello, mq - topic!");
    }

    // 3. fanout
    @RequestMapping(value = "/mq/fanout")
    @ResponseBody
    public Result<String> fanout() {
        mqSender.sendFanout("Hello, mq - fanout!");
        return Result.success("Hello, mq - fanout!");
    }

    // 4. headers
    @RequestMapping(value = "/mq/headers")
    @ResponseBody
    public Result<String> headers() {
        mqSender.sendFanout("Hello, mq - headers!");
        return Result.success("Hello, mq - headers!");
    }

}
