# 一、核心技术栈
1. spring boot + mybatis + druid + redis + thymeleaf + rabbitmq + nginx + jmeter
2. 两次 md5 入库
3. jsr303 参数校验
4. 全局异常处理：@ControllerAdvice + @ExceptionHandler
5. 分布式 session (redis 实现)


# 二、如何使用
1. 新建 miaosha 数据库与数据表并修改 application.properties 中的数据库密码
2. 配置好 redis 并启动 redis-server /usr/local/etc/redis.conf 和 mysql
3. 运行 MiaoshaApplication 浏览器输入 127.0.0.1:8080/login/to_login 进行登录



# 三、开发过程
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
1. 商品表  goods
    - 单纯的商品信息，不包含一些是否是秒杀商品这种字段
    ```
    CREATE TABLE `goods` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '商品id',
      `goods_name` varchar(16) DEFAULT NULL COMMENT '商品名称',
      `goods_title` varchar(64) DEFAULT NULL COMMENT '商品的标题',
      `goods_img` varchar(64) DEFAULT NULL COMMENT '商品的图片',
      `goods_detail` longtext COMMENT '商品详情介绍',
      `goods_price` decimal(10,2) DEFAULT '0.00' COMMENT '商品单价',
      `goods_stock` int(11) DEFAULT '0' COMMENT '库存商品, -1 表示无限制',
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品表';
    ```
2. 订单表   order_info
    ```
    CREATE TABLE `order_info` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
      `goods_id` bigint(20) DEFAULT NULL COMMENT '商品id',
      `delivery_addr_id` bigint(20) DEFAULT NULL COMMENT '收货地址',
      `goods_name` varchar(16) DEFAULT NULL COMMENT '冗余过来的商品商品名称',     // 就可以之间单表查询
      `goods_count` int(11) DEFAULT '0' COMMENT '商品数量',
      `goods_price` decimal(10,2) DEFAULT NULL COMMENT '商品单价',
      `order_channel` tinyint(4) DEFAULT NULL COMMENT '1-pc 2-android 3-ios',
      `status` tinyint(4) DEFAULT '0' COMMENT '0-新建未支付 1-已支付 2-已发货 3-已收获 4-已退款 5-已完成',
      `create_date` datetime DEFAULT NULL COMMENT '订单创建时间',
      `pay_day` datetime DEFAULT NULL COMMENT '支付时间',
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单表';
    ```
3. 秒杀商品表  miaosha_goods
    - 为什么不再商品表添加一个代表是否是秒杀商品的字段？
    - 方便维护，一段时间某个商品参加秒杀活动，一段时间不参加，会使得需要频繁修改。这样可以让`商品表`相对比较稳定
    ```
    CREATE TABLE `miaosha_goods` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '秒杀的商品表',
      `goods_id` bigint(20) DEFAULT NULL COMMENT '商品id',
      `miaosha_price` decimal(10,2) DEFAULT '0.00' COMMENT '秒杀价',
      `stock_count` int(11) DEFAULT NULL COMMENT '库存数量',
      `start_time` datetime DEFAULT NULL COMMENT '秒杀开始时间',
      `end_time` datetime DEFAULT NULL COMMENT '秒杀结束时间',
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='秒杀商品表';
    ```
4. 秒杀订单表    miaosha_order
    ```
    CREATE TABLE `miaosha_order` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
      `order_id` bigint(20) DEFAULT NULL COMMENT '订单id',
      `goods_id` bigint(20) DEFAULT NULL COMMENT '商品id',
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀订单表';
    ```
5. 创建对应的 model 对象

### 3.2 商品列表页 -> 商品详情页
1. GoodsDao -> GoodsService -> GoodsController
2. 查询：商品信息+秒杀信息，为此创建 GoodsVO 结合 Goods + MiaoshaGoods
3. 商品详情页:(1)秒杀未开始或已经结束时，秒杀按钮不能点  (2)秒杀开始时应该有个倒计时

### 3.3 秒杀功能
1. 在商品详情页点击秒杀按钮即将 user 和 goodsId 传入 OrderController 进行处理
    - 编写 MiaoshaService 处理 (1)减库存 -> (2)下订单 -> (3)写入秒杀订单 步骤，需要事务支持
    - 如果秒杀成功则进入 订单详情页 order_detail.html 如果失败则进入 miaosha_fail.html
2. OrderDao -> OrderService -> OrderController miaosha_fail.html order_detail.html
3. miaoshaService.miaosha(miaoshaUser, goods);     进行秒杀：(1)减库存 -> (2)生成订单 -> (3)数据库插入秒杀订单  这三个步骤需要 事务管理
4. 一般提倡在自己的Service下引入自己的Dao(比如说在GoodsService引入GoodsDao)，某Service下想使用其他的Dao则引入对应的Service解决(比如MiaoshaService引入goodsService和orderService)
























