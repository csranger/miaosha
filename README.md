## 项目环境搭建
### 输出结果封装
- 输出结果使用Result.java类封装，为了是代码优雅使用类CodeMsg进一步封装各种异常

### 集成Thymeleaf
- pom依赖 + 配置 + 测试页面

### 集成Mybatis与Druid
- pom依赖 + 配置 + 测试页面
- 数据库事务：已有id为1的记录，先插入id为2的记录和id为1的记录，因为id不可重复，测试代码是否全部回滚

### 集成redis缓存系统
1. 安装redis
- redis启动时需要制定配置文件redis.conf，不指定会使用默认的配置文件
- 修改redis配置文件：
- bind 127.0.0.1 ::1 意思是允许哪些ip可以放完redis服务器，127.0.0.1指只有本地可以访问，这里改成 bind 0.0.0.0允许任意服务器均可访问这个redis
- daemonize no 改成 daemonize yes 允许后台执行
- redis-server /usr/local/etc/redis.conf      启动 redis 服务器
- redis-cli 进行访问
- 关闭redis 需要先进入 redis-cli 执行 shutdown save
- ps -ef | grep redis 查看 redis 进程是否存在
2. 添加 Jedis依赖并配置
3. FastJson依赖： 序列化，将java对象转化成Json字符串写到redis服务器
4. 在DemoController中进行测试
```
@RequestMapping(value = "/redis/set")
    @ResponseBody
    public  Result<String> redisSet() {
        // 键是"k2"，始终是String，值可以是任意类型T，为了存在redis中，将T转化成String，再存
        Boolean ret = redisService.set("k2", "Hello, world!");
        // 键是"k2"，从redis中取出值，并同时告诉get方法转换成什么类型，即存储前值的类型
        String v2 = redisService.get("k2", String.class);
        // 将获得的结果通过 Result 封装使用静态方法输出结果
        return Result.success(v2);
    }
```


