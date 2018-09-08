package com.csranger.miaosha.util;

import java.util.UUID;

public class UUIDUtil {
    // 输出类似于 cf9e0032d7cc4adda988c44fdb997478
    // UUID : 唯一的机器生成的标识
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");     // 默认生成的 UUID 带有 - ，去掉
    }
}
