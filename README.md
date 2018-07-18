## 1. 项目环境搭建
### 1.1 输出结果封装
- 输出结果使用Result.java类封装，为了是代码优雅使用类CodeMsg进一步封装各种异常

### 1.2 集成 Thymeleaf
- pom依赖 + 配置 + 测试页面

### 1.3 集成Mybatis与Druid
- pom依赖 + 配置 + 测试页面
- 数据库事务：已有id为1的记录，先插入id为2的记录和id为1的记录，因为id不可重复，测试代码是否全部回滚

### 1.4 集成redis缓存系统
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
5. 多人开发可能会使得redis中的key会相互重复问题：让不同的模块下的 key 具有不同的前缀

## 2. 登录功能
### 2.1 数据库设计
- 生成数据库表 - miaosha_user 
```
CREATE TABLE `miaosha_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户id，手机号码',
  `nickname` varchar(255) NOT NULL DEFAULT '',
  `password` varchar(32) DEFAULT NULL COMMENT 'MD5(MD5(pass明文+固定salt)+salt）',
  `salt` varchar(10) DEFAULT NULL,
  `head` varchar(128) DEFAULT NULL COMMENT '头像，云存储id',
  `register_date` datetime DEFAULT NULL COMMENT '注册时间',
  `last_login_date` datetime DEFAULT NULL COMMENT '上次登陆时间',
  `login_count` int(11) DEFAULT '0' COMMENT '登陆次数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
### 2.2 明文密码两次md5处理
- 引入 MD5 工具类依赖并编写两次 md5 加密方法 MD5Util

### 2.3 实现登录功能
1. 静态文件 static/ 路径
    - bootstrap 画页面
    - jquery-validation 表单验证
    - layer js 弹框
    - md5 js md5
    - jquery js
    - common.js 是自己编写的js：展示loading框；md5固定盐值
2. 登陆页面 
    - login.html 
3. dao service controller 开发
4. LoginController 中需要进行参数校验：密码是否为空，手机号是否符合格式 之类的检查。这很麻烦，改进就是使用JSR303 参数检验

### 2.4 Js303参数校验+全局异常处理
1. 想要验证LoginVO里的两个参数
    - 在doLogin(@Valid LoginVO loginVO) 加上 @Valid 注解
    - 在 LoginVO 需要验证的属性上加上 验证器 @NotNull  @Length(min=32)
    - 自定义验证器 @IsMobile 
    - 当前缺点：当参数校验不通过无法得知信息。使用全局异常拦截
2. 拦截绑定异常，输出校验不通过的错误信息
    - 处理绑定异常 GlobalExceptionHandler
3. 注意到 MiaoshaUserService login 方法返回的是 CodeMsg 类型
    - 这不合适，该用全局异常/业务异常 GlobalException
    - 同时需要在统一异常处理器进行处理
    
### 2.5 分布式 Session
1. 出现场景：实际应用会有分布式多台应用服务器，如果用户第一个请求落到了第一个服务器上，而第二个请求没有落到这个服务器上，会造成用户session信息丢失
2. 实现 session 功能:服务端将 token 写到 cookie 中，客户端在随后的访问中携带这个 cookie
    - 登陆成功之后，给这个用户生成一个类似于 sessionId 的变量 token 来标识这个用户 -> 写到
    cookie 当中传递给客户端 -> [ 客户端在随后的访问当中都在 cookie 上传这个 token -> 服务端拿到这个 token 之后
    就根据这个tocken取到用户对应的 sesession 信息 ] 后面步骤浏览器来做
3. 服务器给用户设定了 cookie 之后，客户端在随后的访问当中都带有这个值，使用 @CookieValue(value="token") 注解获取到这个值
4. 获取到 token 后从 redis 缓存中取出 user 放入modelMap，传给 /goods/to_list 模版页面
5. 问题1：session 的有效期应该是从最后结束访问的时间开始算：这就要求每打开一个新的页面在从 redis 根据 token 获取到 user 信息
   时重新 addCookie 下，即给这个用户重新生成一个 token 来标识这个用户, 将 token-user 缓存到 redis -> 写到 cookie 当中传递给客户端
6. 问题2：每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息，这就很麻烦,以下就是一个例子

    ```
        @RequestMapping(value = "/to_list")
        public String list(HttpServletResponse response, ModelMap modelMap,
                             @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String cookieToken,
                             @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String paramToken
        ) {
            if (StringUtils.isBlank(cookieToken) && StringUtils.isBlank(paramToken)) {
                return "login";
            }
            // 获取 cookie 中 token
            String token = StringUtils.isBlank(paramToken) ? cookieToken : paramToken;
            // 利用 token 从 redis 拿出 MiaoshaUser 信息
            MiaoshaUser miaoshaUser = miaoshaUserService.getByToken(response, token);
    
            modelMap.put("user", miaoshaUser);
            return "goods_list";
        }
    ```
7. 问题2解决方法：将`每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息`这部分操作移到
   UserArgumentResolver 中，这就意味着不用每个请求页面都写一遍获取 cookie 中 token 获取用户信息这些代码了
   
## 3. 实现秒杀功能
### 3.1  数据库设计
1. 商品表

2. 订单表
3. 秒杀商品表
    - 为什么不再商品表添加一个代表是否是秒杀商品的字段？
    - 方便维护，一段时间某个商品参加秒杀活动，一段时间不参加，会使得需要频繁修改。这样可以让`商品表`相对比较稳定
4. 秒杀订单表

### 3.2 商品列表页 -> 商品详情页 -> 订单详情页























