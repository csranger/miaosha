package com.csranger.miaosha.result;

public class Result<T> {

    // 号码；比如0代表成功，可以从data中读取数据；500100代表着错误信息msg库存不足
    private int code;

    // 错误信息
    private String msg;

    // 成功时的数据，具体数据类型不清楚使用泛型
    private T data;


    // 使用两个静态方法创建 Result 对象
    // 成功时调用;只需传入 T data     成功的信息
    public static <T> Result<T> success(T data) {
        return new Result<T>(data);
    }

    // 失败时调用;只需传入 CodeMsg    CodeMsg 包括 code 和 msg
    public static <T> Result<T> error(CodeMsg cm) {
        return new Result<T>(cm);
    }


    // 构造器,使用上面两个静态方法获取对象，设为private
    private Result(T data) {
        this.code = 0;
        this.msg = "success";
        this.data = data;
    }

    private Result(CodeMsg cm) {
        if (cm == null) {
            return;
        }
        this.code = cm.getCode();
        this.msg = cm.getMsg();
    }


    // getter
    public int getCode() {
        return code;
    }


    public String getMsg() {
        return msg;
    }


    public T getData() {
        return data;
    }

}
