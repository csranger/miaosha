package com.csranger.miaosha.result;

/**
 * 1. CodeMsg 类包括 int code 与 String msg
 * 2. Result 类包括 int code(结果代号:0结果正常，其他结果异常), String msg(异常信息), T data(正确结果时的结果信息)
 * 3. 当 Result 代表错误/异常结果时，使用 CodeMsg.error(CodeMsg cm) 生成异常结果对象
 * 4. 当 Result 代表正常结果时，使用 CodeMsg.success(T data) 生成正常结果信息，因为code 默认就是 0，且不需要异常信息 msg
 * 5. CodeMsg 异常结果信息 对象 全部列出来，这样使用类似 CodeMsg.PPASSWORD_EMPTY 方式产生 CodeMsg 对象
 */
public class CodeMsg {

    private int code;

    private String msg;


    // 通用异常
    public static CodeMsg SUCCESS = new CodeMsg(0, "success");
    public static CodeMsg SERVER_ERROR = new CodeMsg(500100, "服务端异常");
    public static CodeMsg BIND_ERROR = new CodeMsg(500101, "参数校验异常：%s");  // 为这种有 格式化字符串 的添加一个方法

    // 登陆模块 5002XX
    public static CodeMsg PASSWORD_EMPTY = new CodeMsg(500211, "登陆密码不能为空");
    public static CodeMsg MOBILE_EMPTY = new CodeMsg(500212, "手机号不能为空");
    public static CodeMsg MOBILE_ERROR = new CodeMsg(500213, "手机号码格式错误");
    public static CodeMsg MOBILE_NOT_EXIST = new CodeMsg(500214, "手机号码不存在");
    public static CodeMsg PASSWORD_ERROR = new CodeMsg(500215, "密码错误");
    public static CodeMsg SESSION_ERROR = new CodeMsg(500216, "Session 不存在或者已失效");

    // 商品模块 5003XX

    // 订单模块 5004XX
    public static CodeMsg ORDER_NOT_EXIST = new CodeMsg(500400, "订单不存在");


    // 秒杀模块 5005XX
    public static CodeMsg MIAOSHA_OVER = new CodeMsg(500500, "商品已经秒杀完毕");
    public static CodeMsg REPEATE_MIAOSHA = new CodeMsg(500501, "不能重复秒杀");




    // 构造器
    private CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // 处理带有格式化字符串数据
    public CodeMsg fillArgs(Object... args) {
        int code = this.code;
        String msg = String.format(this.msg, args);
        return new CodeMsg(code, msg);
    }


    // getter
    public int getCode() {
        return code;
    }


    public String getMsg() {
        return msg;
    }

    // toString


    @Override
    public String toString() {
        return "CodeMsg{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
