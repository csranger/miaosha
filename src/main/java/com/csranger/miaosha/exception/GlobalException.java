package com.csranger.miaosha.exception;

import com.csranger.miaosha.result.CodeMsg;

/**
 * 当出现异常情况时，就抛出 GlobalException 异常;在 RuntimeException 基础上添加个 CodeMsg
 */
public class GlobalException extends RuntimeException {

    private CodeMsg codeMsg;  // 作为自添加的异常信息



    public GlobalException(CodeMsg cm) {
        super(cm.toString());
        this.codeMsg = cm;
    }


    // getter
    public CodeMsg getCodeMsg() {
        return codeMsg;
    }
}
