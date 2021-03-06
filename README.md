# 一、核心技术栈
1.  spring boot + mybatis + druid + redis + thymeleaf + rabbitmq + jmeter + jquery + ajax
2.  两次 md5 入库
3.  jsr303 参数校验
4.  全局异常处理：@ControllerAdvice + @ExceptionHandler
5.  分布式 session (redis 实现)
6.  Jmeter 模拟多用户同时发起多次秒杀请求比较优化前后QPS
7.  页面级缓存+URL级缓存+对象级缓存
8.  页面静态化与前后端分离: 静态html -> ajax -> controller返回json
9.  redis 预减库存 + rabbitmq异步下单
10. 安全优化：秒杀地址隐藏 + 数学公式验证码 + 接口限流防刷(自定义@AccessLimit注解)


# 二、如何使用
1. 新建 miaosha 数据库与数据表(见/sql目录下sql文件)并修改 application.properties 中的数据库密码
2. 配置好 redis 并启动 redis-server /usr/local/etc/redis.conf 和 mysql
3. 启动 rabbitmq-server
4. 运行 MiaoshaApplication 浏览器输入 127.0.0.1:8080/login/to_login 进行登录 [12345678900 123456]

# 三、项目演示
1. 登陆页面
![登陆页面](./src/main/resources/static/img/登陆页面.png "登陆页面")
2. 商品列表页
![商品列表页](./src/main/resources/static/img/商品列表页.png "商品列表页")
3. 商品详情页
![商品详情页](./src/main/resources/static/img/商品详情页.png "商品详情页")
4. 订单详情页
![订单详情页](./src/main/resources/static/img/订单详情页.png "订单详情页")
5. 验证码错误
![验证码错误](./src/main/resources/static/img/验证码错误.png "验证码错误")
5. 接口限流防刷
![接口限流防刷](./src/main/resources/static/img/接口限流防刷.png "接口限流防刷")


# 四、重点小结
## 总结1：如何集成redis用于存储缓存？
1. 这里没使用RedisTemplate，而是使用原生的 jedis 并自己实现 set get 等功能，序列化使用 fastjson，使用 fastjson 把 java 对象转
换成 json 字符串，存储到 redis server 中；需要系统已安装redis
2. 添加 jedis 和 fastjson 两个依赖
    ```
            <dependency>
                <groupId>redis.clients</groupId>
                <artifactId>jedis</artifactId>
            </dependency>
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>fastjson</artifactId>
                <version>1.2.47</version>
            </dependency>
    ```
3. 在 application.properties 文件中配置下 redis 相关
    ```
    # 连接 redis server 
    redis.host=127.0.0.1
    redis.port=6379
    # 10s
    redis.timeout=10
    redis.password=csranger
    # redis连接池配置：redis访问也是使用连接池
    # 最大连接数
    redis.poolMaxTotal=1000
    # 最大空闲
    redis.poolMaxIdle=500
    # 最大等待
    redis.poolMaxWait=500
    ```
4. 将redis配置加载进来，自定义 RedisConfig 类
    ```java
    @Component                                    // 交由容器管理
    @ConfigurationProperties(prefix = "redis")    // 读取配置文件(application.properties)里以 redis 开头的配置
    @Data                                         // setter getter 方法
    public class RedisConfig {
        private String host;
        private int port;
        private int timeout;           // 秒
        private String password;
        private int poolMaxTotal;
        private int poolMaxIdle;
        private int poolMaxWait;        // 秒
    }
    ```
5. RedisService 类中的 redis 服务(RedisService类的方法)需要一个 Jedis 对象
    - 如何创建这个对象？通过 JedisPool 获取 Jedis 对象，所以需要创建一个 JedisPool 的 Bean 来调用从而生成 Jedis 对象
    - 创建一个 JedisPool 的 Bean，这里需要使用到 redis 的配置：RedisConfig 类
    - 下面生成一个 JedisPool 的 Bean
    ```java
    @Service     // 使用@EnableConfigurationProperties(RedisConfig.class)+RedisPoolFactory构造器也可以
    public class RedisPoolFactory {
    
        @Autowired
        private RedisConfig redisConfig;
    
        // 将 jedis pool Bean 注入到 spring 容器
        @Bean
        public JedisPool jedisPoolFactory() {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(redisConfig.getPoolMaxTotal());
            jedisPoolConfig.setMaxIdle(redisConfig.getPoolMaxIdle());
            jedisPoolConfig.setMaxWaitMillis(redisConfig.getPoolMaxWait() * 1000);
            JedisPool jp = new JedisPool(jedisPoolConfig, redisConfig.getHost(), redisConfig.getPort(),
                    redisConfig.getTimeout() * 1000, redisConfig.getPassword(), 0);
            return jp;
        }
    }
    ```
6. 编写 RedisService 类提供 redis 服务
    - get()       从 redis 服务器中获取对象
    - set()       将一个 Bean 对象转化成 String 写入到 redis 中
    - exists()    判断一个 key 是否存在于 redis 中
    - incr() 
    - decr() 
    - delete()    删除指定键对应的缓存
    ```java
    @Service
    public class RedisService {
    
        @Autowired
        JedisPool jedisPool;
    
    
        /**
         * 1。从 redis 服务器中获取对象，例如User对象，键可以是id，name等
         */
        public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                // realKey
                String realKey = prefix.getPrefix() + key;
                String str = jedis.get(realKey);
                T t = stringToBean(str, clazz);
                return t;
            } finally {
                returnToPool(jedis);
            }
        }
    
    
        /**
         * 将 字符串 转化成一个 对象
         * 作为工具类，rabbitmq 中也会用到
         */
        public static <T> T stringToBean(String s, Class<T> clazz) {
            if (s == null || s.length() <= 0 || clazz == null) {
                return null;
            }
            if (clazz == int.class || clazz == Integer.class) {
                return (T) Integer.valueOf(s);
            } else if (clazz == String.class) {
                return (T) s;
            } else if (clazz == long.class || clazz == Long.class) {
                return (T) Long.valueOf(s);
            } else {    // 其他类型认为它是一个 Bean，利用 fastjson 转换成 String
                return JSON.toJavaObject(JSON.parseObject(s), clazz);
            }
        }
    
    
        /**
         * 2。使用 fastjson 将一个 Bean 对象转化成 String 写入到 redis 中
         */
        public <T> boolean set(KeyPrefix prefix, String key, T value) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                String str = beanToString(value);
                if (str == null || str.length() <= 0) {
                    return false;
                }
                // realKey
                String realKey = prefix.getPrefix() + key;
                int seconds = prefix.expireSeconds();   // 过期时间
                if (seconds <= 0) {    // 意味着永不过期
                    jedis.set(realKey, str);
                } else {                // 设置了过期时间，存在redis时使用 setex 方法也传递一个过期时间
                    jedis.setex(realKey, seconds, str);
                }
                return true;
            } finally {
                returnToPool(jedis);
            }
        }
    
    
        /**
         * 将 对象 转化成 字符串
         * 作为工具类，rabbitmq 中也会用到
         */
        public static <T> String beanToString(T value) {
            if (value == null) {
                return null;
            }
            Class<?> clazz = value.getClass();
            if (clazz == int.class || clazz == Integer.class) {
                return "" + value;
            } else if (clazz == String.class) {
                return (String) value;
            } else if (clazz == long.class || clazz == Long.class) {
                return "" + value;
            } else {    // 其他类型认为它是一个 Bean，利用 fastjson 转换成 String：例如 MiaoshaUser 对象
                return JSON.toJSONString(value);
            }
        }
    
    
        // 两个方法的工具方法，释放 Jedis 到 JedisPool
        private void returnToPool(Jedis jedis) {
            if (jedis != null) {
                jedis.close();     // 返回到连接池中
            }
        }
    
        /**
         * 3. 判断一个 key 是否存在于 redis 中
         */
        public <T> boolean exists(KeyPrefix prefix, String key) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                // realKey
                String realKey = prefix.getPrefix() + key;
                return jedis.exists(realKey);
            } finally {
                returnToPool(jedis);
            }
        }
    
    
        /**
         * 4. increase
         * Increment the number stored at key by one. If the key does not exist or contains a value of a
         * wrong type, set the key to the value of "0" before to perform the increment operation.
         */
        public <T> Long incr(KeyPrefix prefix, String key) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                // realKey
                String realKey = prefix.getPrefix() + key;
                return jedis.incr(realKey);
            } finally {
                returnToPool(jedis);
            }
        }
    
    
        /**
         * 5. decrease
         */
        public <T> Long decr(KeyPrefix prefix, String key) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                // realKey
                String realKey = prefix.getPrefix() + key;
                return jedis.decr(realKey);
            } finally {
                returnToPool(jedis);
            }
        }
    
        /**
         * 6. delete 删除指定键对应的缓存
         */
        public boolean delete(KeyPrefix prefix, String key) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                // realKey
                String realKey = prefix.getPrefix() + key;
                long ret = jedis.del(realKey);
                return ret > 0;
            } finally {
                returnToPool(jedis);
            }
        }
    
    }
    ```
7. 将键值对存储在 redis 中，将 真正的键 分成 前缀 和 键，前缀用于表示存储的键值对来自哪个模块，比如说商品模块，订单模块等等
    - KeyPrefix
    ```java
    public interface KeyPrefix {
    
        int expireSeconds();
    
    
        String getPrefix();
    }
    ```
    - BasePrefix
    ```java
    public abstract class BasePrefix implements KeyPrefix {
    
        private int expireSeconds;    // redis 存储有效期，单位 s, 在MiaoshaService里让其等于 cookie 有限期
    
        private String prefix;
    
    
        // public 构造器，但是抽象类不可用于new一个对象
        public BasePrefix(String prefix) {
            this(0, prefix);   // 0表示永不过期
        }
    
        public BasePrefix(int expireSeconds, String prefix) {
            this.expireSeconds = expireSeconds;
            this.prefix = prefix;
        }
    
    
        // 获取两个属性的值的方法
        @Override
        public int expireSeconds() {    // 默认0，代表永不过期
            return expireSeconds;
        }
    
        @Override
        public String getPrefix() {
            String className = getClass().getSimpleName();   // 不会像 getName() 方法一样加上包名
            return className + ":" + prefix;
        }
    }
    ```
    - GoodsKey 商品模块的前缀
    ```java
    public class GoodsKey extends BasePrefix {   
        // 私有化构造器，控制 页面缓存前缀 对象的数量
        private GoodsKey(int expireSeconds, String prefix) {
            super(expireSeconds, prefix);
        }   
        // 列出所以 页面缓存前缀 对象
        // 以下 2 个实例: 默认缓存过期时间 60s
        public static GoodsKey getGoodsList = new GoodsKey(60, "gl");
        public static GoodsKey getGoodsDetail = new GoodsKey(60, "gd");
        public static GoodsKey getMiaoshaGoodsStock = new GoodsKey(0, "gs");   
    }
    ```
    - OrderKey 订单模块的前缀
    ```java
    public class OrderKey extends BasePrefix {  
        // 私有化构造器，控制 订单缓存前缀 对象的数量,永不过期
        private OrderKey(String prefix) {
            super(prefix);
        }   
        // 列出所以 订单缓存前缀 对象
        public static OrderKey getMiaoshaOrderByUserIdGoodsId = new OrderKey("MiaoshaOrder");   
    }
    ```

## 总结2：分布式 Session 如何实现？<a name="2"></a>
1. 流程：用户登陆，服务端生成一个 "token" 值 uuid 标识这个用户，将 token - user 键值对缓存到 redis 中，同时将token值放入用户请求的响应response里
客户端在之后的访问中就会携带这个 cookie，服务端根据这个token就可以在redis中知道是哪个用户。注意点就是session的过期时间应根据最后一次访问开始算。
2. 考虑一个问题：每打开不同页面，发出不同请求，都需要验证token，例如在商品列表页需要验证token，在商品详情页也需要验证 token，代码会重复，也很啰嗦。
希望在controller层中的方法里有个 user 参数，和 HttpServletRequest request, Model model 参数一样，这样使用 if (user == null) 来判断用户是否登陆，而不是
使用request里的 cookie 到 redis 里查user是否存在。
3. 用户登录时需要生成 uuid 作为 "token" 的值，将 token-user 存到redis，同时将token写入response中的cookie里
    - 处理 login 请求
    ```java
    @Controller
    @RequestMapping(value = "/login")
    public class LoginController {   
        @Autowired
        private MiaoshaUserService miaoshaUserService;
        @RequestMapping(value = "/do_login")
        @ResponseBody
        public Result<String> doLogin(HttpServletResponse response, @Valid LoginVO loginVO) {
            String token = miaoshaUserService.login(response, loginVO);    // 生成 uuid 作为 "token" 的值，将 token-user 存到redis，同时将token写入response中的cookie里
            return Result.success(token);
        }
    
    }
    ```
    - login 方法：将 token-user 存到redis，同时将token写入response中的cookie里
    ```
    public String login(HttpServletResponse response, LoginVO loginVO) {
            if (loginVO == null) {
                throw new GlobalException(CodeMsg.SERVER_ERROR);
            }
            // 1.判断手机号是否存在于数据库中
            String password = loginVO.getPassword();
            String mobile = loginVO.getMobile();
            MiaoshaUser miaoshaUser = getById(Long.parseLong(mobile));     // 用户信息
            if (miaoshaUser == null) {
                throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
            }
            // 2. 如果手机号存在于数据库中,进行密码匹配
            String dbPass = miaoshaUser.getPassword();
            String dbSalt = miaoshaUser.getSalt();
            String pass = MD5Util.fromPassToDBPass(password, dbSalt);
            // 2.1 如果密码不匹配，登陆失败
            if (!dbPass.equals(pass)) {
                throw new GlobalException(CodeMsg.PASSWORD_ERROR);
            }
            // 2.2 密码匹配，意味着登陆成功
    
            // 生成 cookie/实现 session 功能：登陆成功之后，给这个用户生成一个类似于 sessionId 的变量 token 来标识这个用户 -> 写到
            // cookie 当中传递给客户端 -> [ 客户端在随后的访问当中都在 cookie 上传这个 token -> 服务端拿到这个 token 之后
            // 就根据这个 token 取到用户对应的 sesession 信息 ] 后面步骤浏览器来做
            String token = UUIDUtil.uuid();     // 用户登录后将此用户 token 记住不需要没打开一个页面都生成一个新的 token
            addCookie(response, token, miaoshaUser);
            logger.info("生成 token 放入 cookie，写到 response 中发送给客户端");
    
            return token;
    
        }
    
    
        /**
         * 登陆成功之后，给这个用户生成一个 token 来标识这个用户
         * (1) 将 token-user 缓存到 redis
         * (2) 同时将token写到 cookie 当中传递给客户端response
         */
        private void addCookie(HttpServletResponse response, String token, MiaoshaUser miaoshaUser) {
            // 1 将 token-user 再次写到 redis 缓存中，前缀已经设定了存储到 redis 中的过期时间：相当于刷新了 过期时间
            redisService.set(MiaoshaUserKey.token, token, miaoshaUser);
    
            // 2 token 放入 cookie
            Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
            cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());      // cookie 的有效期设置为 MiaoshaUserKey.token.expireSeconds() 即让其等于 redis 保存有效期期
            cookie.setPath("/");
    
            // 3 写到 response ，所以参数里加上 HttpServletResponse,service中方法不可直接加上HttpServletResponse；需要在Controller中加上，类似于 ModelMap map
            response.addCookie(cookie);
        }
    ```
    - 生成 uuid 的工具类
    ```java
    public class UUIDUtil {
        // 输出类似于 cf9e0032d7cc4adda988c44fdb997478
        // UUID : 唯一的机器生成的标识
        public static String uuid() {
            return UUID.randomUUID().toString().replace("-", "");     // 默认生成的 UUID 带有 - ，去掉
        }
    }
    ```
4. 打开其他页面，request 里会自带 token，希望在controller层中的方法里有个 user 参数，和 HttpServletRequest request, Model model 参数
一样，这样使用 if (user == null) 来判断用户是否登陆
    - 新建 UserArgumentResolver 类实现 controller 方法里带上 user 这个参数
    - controller 方法里带上 user 这个参数在 resolveArgument 方法里利用 request 的 生成 cookie 在redis里找到这个对象，找不到意味着没登陆
    ```java
    @Service
    public class UserArgumentResolver implements HandlerMethodArgumentResolver {
    
        @Autowired
        private MiaoshaUserService miaoshaUserService;
    
        /**
         * resolver 支持 MiaoshaUser 参数
         */
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            Class<?> clazz = parameter.getParameterType();    // 获取参数类型
            return clazz == MiaoshaUser.class;
        }
    
    
        /**
         * 生成 MiaoshaUser 对象作为 controller 方法里的参数
         */
        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
    
            // 每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息
            // 如果cookie里没有token(直接打开页面没有登陆的情况)，getByToken 方法从redis中去不成对象，返回 null
            String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);      // "token"值 被放在请求参数中获取
            String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);  // "token"值 被放在 cookie 中获取
            if (StringUtils.isBlank(paramToken) && StringUtils.isBlank(cookieToken)) {
                return null;
            }
            String token = StringUtils.isBlank(paramToken) ? cookieToken : paramToken;
            return miaoshaUserService.getByToken(response, token);     // 从 redis 中利用 "token" 对应的值 UUID 取出 MiaoshaUser 对象
        }
    
        /**
         * 从用户请求中的 cookie 中取出 token 变量的值(UUID)
         * 从 request 中取出 "token" 对应的值 UUID
         */
        private String getCookieValue(HttpServletRequest request, String cookieName) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null || cookies.length <= 0) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
            return null;
        }
    }
    ```   
5. 需要将这个 UserArgumentResolver 注册到系统上，注册到 WebConfig 类上
    ```java
    // WebMvcConfigurerAdapter 过时，这里使用里 WebMvcConfigurer 接口
    @Configuration
    public class WebConfig implements WebMvcConfigurer {
    
        @Autowired
        private UserArgumentResolver userArgumentResolver;   
    
        /**
         * UserArgumentResolver 注册到系统
         */
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(userArgumentResolver);
        }
    }
    ```
6. 使用：这样就可以在 controller 里方法参数里加上 MiaoshaUser user 参数了，通过 if (user == null) 判断用户是否已经登陆

## 总结3：如何实现接口限流防刷，防止用户使用程序短时间内多次请求某个接口，例如短时间大量进行秒杀请求？
1. 考虑的问题是否需要用户登录；实现功能如限制5s内只允许发起10次请求；希望将这个非业务代码和业务逻辑分开，使用自定义注解，加了此注解就进行此接口的限流防刷
2. 自定义一个注解 @AccessLimit
    ```
    @Retention(RUNTIME)             // 注解的的存活时间 注解可以保留到程序运行的时候
    @Target(METHOD)                 // @Target 指定了注解运用的地方 可以给方法进行注解
    public @interface AccessLimit {
        int seconds();                           // 时间
        int maxCounts();                         // 在指定时间内的最大访问次数
        boolean needLogin() default true;        // 是否需要登录 默认需要登录
    }
    ```
3. 实现一个访问接口拦截器来处理这个注解 AccessInterceptor 
    ```java
    /**
     * 接口拦截器 AccessInterceptor 来处理 @AccessLimit 注解
     */
    @Service   // 拦截器交由容器管理
    public class AccessInterceptor extends HandlerInterceptorAdapter {   // 继承拦截器基类
        @Autowired
        private MiaoshaUserService miaoshaUserService;
    
        @Autowired
        private RedisService redisService;
    
        // 添加 @AccessLimit 注解的方法执行前进行拦截，进行一些处理，重写继承拦截器基类的 preHandle 方法
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (handler instanceof HandlerMethod) {
    
                // 1. 获取用户对象：利用 request
                MiaoshaUser miaoshaUser = getUser(request, response);
    
    
                // 2. 获取注解 @AccessLimit 注解信息
                HandlerMethod hm = (HandlerMethod) handler;
                AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);   // 获取方法上的 @AccessLimit 注解
                if (accessLimit == null) {      // 方法上不存在 @AccessLimit 注解则不需要进行任何限制
                    return true;
                }
                // 如果方法上存在 @AccessLimit 注解，则获取注解的限制信息
                int seconds = accessLimit.seconds();
                int maxCounts = accessLimit.maxCounts();
                boolean needLogin = accessLimit.needLogin();
    
                // 3. 拦截器处理 @AccessLimit
                // 3.1 处理 needLogin
                String key = request.getRequestURI();
                if (needLogin) {  // 如果需要登录则判断 miaoshaUser 是否为空
                    if (miaoshaUser == null) {
                        render(response, CodeMsg.SESSION_ERROR);
                        return false;
                    }
                    key += "_" + miaoshaUser.getId();
                }
                // 3.2 处理 seconds maxCount
                AccessKey ak = AccessKey.withExpires(seconds);
                Integer count = redisService.get(ak, key, Integer.class);
                if (count == null) {
                    redisService.set(ak, key, 1);
                } else if (count < maxCounts) {
                    redisService.incr(ak, key);
                } else {
                    render(response, CodeMsg.ACCESS_LIMIT_REACHED);
                    return false;
                }
    
            }
            return super.preHandle(request, response, handler);
        }
    
        /**
         * 根据用户的 request 取出用户
         * request 如果 包含 cookie 中了包含了 token 则从 redis 中取出 MiaoshaUser 对象
         */
        private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
            // 每打开一个页面都需要先获取请求信息 cookie 里的 token，然后从 redis 根据 token 获取到 user 信息
            // 如果cookie里没有token(直接打开页面没有登陆的情况)，getByToken 方法从redis中去不成对象，返回 null
            String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);      // "token"值 被放在请求参数中获取
            String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);  // "token"值 被放在 cookie 中获取
            if (StringUtils.isBlank(paramToken) && StringUtils.isBlank(cookieToken)) {
                return null;
            }
            String token = StringUtils.isBlank(paramToken) ? cookieToken : paramToken;
            return miaoshaUserService.getByToken(response, token);     // 从 redis 中利用 "token" 对应的值 UUID 取出 MiaoshaUser 对象
    
        }
    
    
        /**
         * 从用户请求中的 cookie 中取出 token 变量的值(UUID)
         * 从 request 中取出 "token" 对应的值 UUID
         */
        private String getCookieValue(HttpServletRequest request, String cookieName) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null || cookies.length <= 0) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
            return null;
        }
    
        /**
         * 利用 response 输出 CodeMsg 信息
         */
        public void render(HttpServletResponse response, CodeMsg codeMsg) throws Exception {
            OutputStream out = response.getOutputStream();
            String str = JSON.toJSONString(codeMsg);
            out.write(str.getBytes("UTF-8"));
            out.flush();
            out.close();
        }
    }

    ```
4. 拦截器需要注册到系统上，注册实现和 UserArgumentResolver 一样，UserArgumentResolver也是注册到 WebConfig 类上
    ```java
    // WebMvcConfigurerAdapter 过时，这里使用里 WebMvcConfigurer 接口
    @Configuration
    public class WebConfig implements WebMvcConfigurer {
    
        @Autowired
        private UserArgumentResolver userArgumentResolver;
    
        @Autowired
        private AccessInterceptor accessInterceptor;   
     
        /**
         * UserArgumentResolver 注册到系统
         */
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(userArgumentResolver);
        }
    
        /**
         * AccessInterceptor 拦截器注册到系统
         */
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(accessInterceptor);
        }
    }
    ```
5. 将 @AccessLimit 注解到想进行限流的接口上即可使用
    - MiaoshaController 上 /miaosha/path 请求上加上此注解实现5s内最多进行5次请求，且需要用户登录
    ```
        @AccessLimit(seconds = 5, maxCounts = 5, needLogin = true)
        @RequestMapping(value = "/path", method = RequestMethod.GET)
        @ResponseBody
        public Result<String> getMiaoshaPath(MiaoshaUser miaoshaUser,
                                             @RequestParam("goodsId") long goodsId,
                                             @RequestParam("verifyCode") int verifyCode) {
            logger.info("正在获取秒杀地址");
            if (miaoshaUser == null) {
                return Result.error(CodeMsg.SESSION_ERROR);
            }
    
            // 验证码是否正确: miaoshaUser, goodsId 是为了从 redis 中取出答案和 verifyCode 进行对比
            boolean check = miaoshaService.checkVerifyCode(miaoshaUser, goodsId, verifyCode);
            if (!check) {
                return Result.error(CodeMsg.REQUEST_ILLEGAL);
            }
    
            // 生成一个随机数作为秒杀请求地址，返回给客户端，客户端才知道秒杀地址请求秒杀 + 将这个随机值暂时缓存在 redis，以确认秒杀地址是否正确
            String path = miaoshaService.createPath(miaoshaUser, goodsId);
    
            return Result.success(path);
        }
    ```
## 总结4：如何在spring-boot使用 rabbitmq 消息队列？
1. 需要先安装rabbitmq，添加 spring-boot-starter-amqp 依赖
2. rabbitmq 在 application.properties 中进行配置
    ```
    # 连接到 rabbitmq-server
    spring.rabbitmq.host=127.0.0.1
    spring.rabbitmq.port=5672
    spring.rabbitmq.username=guest
    spring.rabbitmq.password=guest
    spring.rabbitmq.virtual-host=/
    # 消费者数量，加快出队速度
    spring.rabbitmq.listener.simple.concurrency=10
    spring.rabbitmq.listener.simple.max-concurrency=10
    # 链接从队列里取，每次取几个
    spring.rabbitmq.listener.simple.prefetch=10
    # 默认 listener 消费者自动启动
    spring.rabbitmq.listener.simple.auto-startup=true
    # 消费者消费失败后会重新将数据压入队列
    spring.rabbitmq.listener.simple.default-requeue-rejected=true
    # 队列满了，放不进去，启动重试
    spring.rabbitmq.template.retry.enabled=true
    # 1s 后重试一次
    spring.rabbitmq.template.retry.initial-interval=1000ms
    # 重试最大 3 次
    spring.rabbitmq.template.retry.max-attempts=3
    # 重试最大间隔 10s
    spring.rabbitmq.template.retry.max-interval=10000ms
    # 如果值为2，第一次等1s，第二次等2s，第三次等4s...
    spring.rabbitmq.template.retry.multiplier=1
    ```
3. 配置一些Bean：消息队列Queue的Bean；交换机 Exchange 的Bean；队列和交换机之间的绑定的Bean
    ```java
    /**
     * 配置一些 Bean：消息队列Queue + 交换机Exchange + 队列和交换机之间的绑定
     */
    @Configuration
    public class MQConfig {
    
        // 队列名，交换机名 全部列出
        public static final String DIRECT_QUEUE = "direct.queue";        // direct 队列名
    
        public static final String TOPIC_QUEUE1 = "topic.queue1";        // Topic 队列名
        public static final String TOPIC_QUEUE2 = "topic.queue2";
        public static final String TOPIC_EXCHANGE = "topicExchange";     // Topic 交换机名
    
        public static final String MIAOSHA_QUEUE = "miaosha.queue";       // 秒杀 queue 队列
    
    
        /**
         * 1. Direct 模式
         */
    
        /**
         * Direct 队列
         */
        @Bean
        public Queue directQueue() {
            return new Queue(DIRECT_QUEUE, true);
        }
    
        /**
         * 秒杀队列Queue， Direct 模式
         */
        @Bean
        public Queue miaoshaQueue() {
            return new Queue(MIAOSHA_QUEUE, true);
        }
    
    
        /**
         * 2. Topic 模式 交换机Exchange
         * 先把消息放入 Exchange
         */
    
        /**
         * Topic 队列
         */
        @Bean
        public Queue topicQueue1() {
            return new Queue(TOPIC_QUEUE1, true);
        }
    
        @Bean
        public Queue topicQueue2() {
            return new Queue(TOPIC_QUEUE2, true);
        }
    
        /**
         * Topic交换机Exchange
         */
        @Bean
        public TopicExchange topicExchange() {
            return new TopicExchange(TOPIC_EXCHANGE);
        }
    
        /**
         * Topic交换机和Queue进行绑定
         * 向 交换机 传信息时还需一个 routingKey 参数，如果放"topic.key1"值，两个绑定均满足，所以这个信息会放到两个队列中 topicQueue1 和 topicQueue2
         * 如果放"topic.key2"值，只有topicBinding2绑定满足，所以这个信息只会放入 topicQueue2 队列中
         */
        @Bean
        public Binding topicBinding1() {
            return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with("topic.key1");
        }
    
        @Bean
        public Binding topicBinding2() {
            return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with("topic.#");
        }   
    }
    ```
    
4. 创建消息发送者和消息接受者
    - 消息发送者
    ```java
    /**
     * 向指定 队列 或 交换机发送数据
     */
    @Service
    @Slf4j   // lombok 快捷注解
    public class MQSender {   
        // 操作 mq 的帮助类
        @Autowired
        private AmqpTemplate amqpTemplate;
    
        /**
         * 1. Direct 模式
         * 指定向名为 DIRECT_QUEUE 的队列发送数据
         */
        public void sendDirect(Object message) {
            // 对象转化成字符串，之前 redis 中写过 beanToString 方法，利用 fastjson 依赖
            String msg = RedisService.beanToString(message);
            log.info("send message: " + message);
            amqpTemplate.convertAndSend(MQConfig.DIRECT_QUEUE, msg);   // 指定发送到哪个 Queue
        }
    
        /**
         * 2. Topic 模式 交换机Exchange
         * 指定向名为 TOPIC_EXCHANGE 的交换机发送数据(因为交换机是和队列绑定在一起的)
         */
        public void sendTopic(Object message) {
            // 对象转化成字符串，之前 redis 中写过 beanToString 方法，利用 fastjson 依赖
            String msg = RedisService.beanToString(message);
            log.info("send topic message: " + message);
            amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key1", msg + 1);
            amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key2", msg + 2);
        }
        
        // 使用 Direct 模式发送秒杀信息
        public void sendMiaoshaMessage(MiaoshaMessage miaoshaMessage) {
            // 对象转化成字符串，之前 redis 中写过 beanToString 方法，利用 fastjson 依赖
            String message = RedisService.beanToString(miaoshaMessage);
            log.info("send message: " + message);
            // 放入名为 DIRECT_QUEUE 的队列
            amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE, message);
        }   
    }
    ```
    - 消息接受者：监听指定队列
    ```java
    /**
     * 从队列中取出数据
     */
    @Service
    @Slf4j
    public class MQReceiver {
        @Autowired
        private GoodsService goodsService;
    
        @Autowired
        private OrderService orderService;
    
        @Autowired
        private MiaoshaService miaoshaService;   
           
        // 接收 miaosha.queue 队列发送的消息
        @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
        public void receiveMiaoshaQueue(String message) {
            log.info("receive miaosha message: " + message);
            // 4. 秒杀请求出队，生成订单，减少库存：将收到 MiaoshaMessage 消息还原成对象，获取秒杀信息
            MiaoshaMessage miaoshaMessage = RedisService.stringToBean(message, MiaoshaMessage.class);
            MiaoshaUser miaoshaUser = miaoshaMessage.getMiaoshaUser();
            long goodsId = miaoshaMessage.getGoodsId();
            // 判断库存，这里访问了数据库，这一步很少请求可以进来；没库存就返回，什么都不做
            log.info("从rabbirmq队列中取出秒杀信息，判断此商品是否还有库存");
            GoodsVO goodsVO = goodsService.getGoodsVOByGoodsId(goodsId);
            int stock = goodsVO.getStockCount();      // 注意这里是 getStockCount 不是 getGoodsStock
            if (stock < 0) {
                return;
            }
            // 有库存判断是否重复秒杀
            MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
            if (miaoshaOrder != null) {
                return;
            }
            // 减库存+生成秒杀订单
            miaoshaService.miaosha(miaoshaUser, goodsVO);
        }
    }
    ```

    
## 总结5：redis 预减库存和rabbitmq 异步下单
1. 秒杀一般实现流程：(1)判断用户是否登陆(2)查goods数据库判断是否有库存(3)查order数据库判断是否已经有订单了，防止重复秒杀(4)减库存+生成订单
2. redis 预减库存和rabbitmq 异步下单流程：(1)系统初始化时，把商品数量加载到redis 即商品id goodsId 是键，库存数量 stockCount 是值
(2)收到请求，redis预减库存，库存不足直接返回：会使得后面的请求不再访问数据库，大大减少数据库的压力(3)将秒杀请求放入消息队列中异步下单，请求会
立即返回"排队中"，这样客户端会立刻收到响应(4)请求出队，减库存+生成订单，生成完订单写入到redis缓存中，这样客户端就可以立即查询到订单了
(5)客户端不断请求服务端查询是否秒杀成功，这一步与(4)并发进行
3. MiaoshaController 中处理秒杀请求的方法(1)(2)(3)步骤
    ```java
    public class MiaoshaController implements InitializingBean {
        // redis 预减库存+rabbitmq异步下单 优化秒杀功能
        // InitializingBean 接口抽象方法：容器启动时，发现实现此接口会回调这个抽象方法
        @Override
        public void afterPropertiesSet() throws Exception {
            // 1. 系统启动时就把商品库存数量加载到 redis:每个秒杀商品id是键，对应商品的库存是值
            // 同时利用 localOverMap 在本地标记每个商品有库存，可秒杀的
            List<GoodsVO> goodsVOList = goodsService.listGoodsVO();
            if (goodsVOList == null) {
                return;
            }
            for (GoodsVO goodsVO : goodsVOList) {
                redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goodsVO.getId(), goodsVO.getStockCount());
                localOverMap.put(goodsVO.getId(), false);   // 商品标记此商品秒杀没有结束
            }
    
        }
        
        @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
        @ResponseBody
        public Result<Integer> miaosha(MiaoshaUser miaoshaUser,
                                       @RequestParam("goodsId") long goodsId,
                                       @PathVariable("path") String path) {
            logger.info("用户 " + miaoshaUser.getId() + " 正在秒杀，秒杀商品的id是 " + goodsId);
    
            if (miaoshaUser == null) {
                return Result.error(CodeMsg.SESSION_ERROR);
            }
            // 验证{path} 键是 miaoshaUser.getId() + "_" + goodsId
            boolean check = miaoshaService.checkPath(miaoshaUser, goodsId, path);
            if (!check) {   // 验证失败，返回请求非法
                return Result.error(CodeMsg.REQUEST_ILLEGAL);
            }
    
            // 本地缓存记录商品是否秒杀结束：减少秒杀库存没了后之后的用户依然发起秒杀请求对redis的访问
            boolean over = localOverMap.get(goodsId);
            if (over) {
                return Result.error(CodeMsg.MIAOSHA_OVER);
            }
    
            // 2. 收到请求，redis 预减库存，库存不足，直接返回，否则继续；返回的是剩下的库存
            // 可能会出现单个用户同时发起多个请求，减库存了，但是真正生成订单的只有1个，这可以通过验证码防止，另外卖超是不允许的，卖不完是允许的
            long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
            if (stock < 0) {
                localOverMap.put(goodsId, true);
                return Result.error(CodeMsg.MIAOSHA_OVER);
            }
            // 判断是否秒杀过了：根据 userId 和 goodsId 查询是否有订单存在
            MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
            if (miaoshaOrder != null) {
                return Result.error(CodeMsg.REPEATE_MIAOSHA);
            }
            // 3. 秒杀请求压入 rabbitmq 队列，立即返回排队中(无阻塞)
            MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
            miaoshaMessage.setGoodsId(goodsId);
            miaoshaMessage.setMiaoshaUser(miaoshaUser);
            mqSender.sendMiaoshaMessage(miaoshaMessage);
            return Result.success(0);   // 秒杀请求压入 rabbitmq 队列，立即返回，无阻塞
        }
    }
    ```
4. 从队列中取出秒杀信息MiaoshaMessage对象，只有两个属性 user 和 goodsId
    ```java
    /**
     * 从队列中取出数据
     */
    @Service
    public class MQReceiver {
        @Autowired
        private GoodsService goodsService;
    
        @Autowired
        private OrderService orderService;
    
        @Autowired
        private MiaoshaService miaoshaService;
        
        // 接收 miaosha.queue 队列发送的消息
        @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
        public void receiveMiaoshaQueue(String message) {
            log.info("receive miaosha message: " + message);
            // 4. 秒杀请求出队，生成订单，减少库存：将收到 MiaoshaMessage 消息还原成对象，获取秒杀信息
            MiaoshaMessage miaoshaMessage = RedisService.stringToBean(message, MiaoshaMessage.class);
            MiaoshaUser miaoshaUser = miaoshaMessage.getMiaoshaUser();
            long goodsId = miaoshaMessage.getGoodsId();
            // 判断库存，这里访问了数据库，这一步很少请求可以进来；没库存就返回，什么都不做，这个判断库存是访问数据库来的，不是redis
            log.info("从rabbirmq队列中取出秒杀信息，判断此商品是否还有库存");
            GoodsVO goodsVO = goodsService.getGoodsVOByGoodsId(goodsId);
            int stock = goodsVO.getStockCount();      // 注意这里是 getStockCount 不是 getGoodsStock
            if (stock < 0) {
                return;
            }
            // 有库存判断是否重复秒杀
            MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
            if (miaoshaOrder != null) {
                return;
            }
            // 减库存+生成秒杀订单
            miaoshaService.miaosha(miaoshaUser, goodsVO);
        }
    }
    ```
5. 客户端需要不断请求服务端查看秒杀是否成功
    - 如何判断是在排队还是秒杀失败？数据库查不到订单原因 1. 秒杀失败 2. 在 rabbitmq 队列中，排队中，还没执行到   如何区分？
    - ajax 发起里查询秒杀结果的请求，说明秒杀请求已经提交到rabbitmq队列，如果还有此商品的库存，则应该是在排队中，反之秒杀失败
    - 所以根据秒杀商品是否还有库存来判断是秒杀失败还是排队中
    ```javascript
    function getMiaoshaResult() {
            g_showLoading();
            $.ajax({
                url: "/miaosha/result",
                type: "GET",
                data: {
                    goodsId: $("#goodsId").val()
                },
                success: function (data) { // data 指 Result.success(result) 对象 orderId:秒杀成功； -1:秒杀失败； 0:排队中，客户端继续轮询
                    if (data.code == 0) {
                        var result = data.data;   // data.data 指 result：orderId -1 0
                        if (result < 0) {
                            layer.msg("对不起，秒杀失败");
                        } else if (result == 0) {   // 排队中则需要再次发送请求
                            setTimeout(function () {    // 200ms后再次请求获取秒杀结果
                                getMiaoshaResult(goodsId);
                            }, 200)
    
                        } else {
                            // 两个按钮，及其对应的回调
                            layer.confirm("恭喜你，秒杀成功！查看订单？", {btn: ["确定", "取消"]},
                                function () {
                                    window.location.href = "/order_detail.htm?orderId=" + result;
                                }, function () {
                                    layer.closeAll();
                                });
                        }
    
                    } else {
                        layer.msg(data.msg);
                    }
                },
                error: function () {
                    layer.msg("客户端请求有误.")
                }
            });
        }
    ```
    - 处理 /miaosha/result 请求 MiaoshaController
    ```
    /**
         * 查询秒杀结果：如果秒杀成功，数据库里有订单记录，则返回订单id
         * orderId : 秒杀成功
         * -1      : 秒杀失败
         * 0       : 排队中，客户端继续轮询
         */
        @RequestMapping(value = "/result", method = RequestMethod.GET)
        @ResponseBody
        public Result<Long> result(MiaoshaUser miaoshaUser, @RequestParam("goodsId") long goodsId) {
            logger.info("正在获取秒杀结果订单");
            if (miaoshaUser == null) {
                return Result.error(CodeMsg.SESSION_ERROR);
            }
            // 查询是否生成里订单:如果秒杀成功，数据库里有订单记录，则返回订单id
            long result = miaoshaService.getMiaoshaResult(miaoshaUser.getId(), goodsId);
    
            return Result.success(result);
        }
    ```
    - MiaoshaService
    ```
        /**
         * orderId : 秒杀成功，数据库中可以查到订单，返回订单id即可
         * -1      : 秒杀失败，意味着商品秒杀卖完了
         * 0       : 排队中，客户端继续轮询，再次查询秒杀结果
         * 注意点：秒杀失败和排队中两种情况下均查不到订单，如何区分开来？
         * 查不到订单情况下，如果该商品没有库存，说明秒杀失败，如果该商品有库存，说明还在队列中
         */
        public long getMiaoshaResult(long userId, long goodsId) {
            // 从数据库里查是否生成了订单
            MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
            if (miaoshaOrder != null) {   // 生成了订单 秒杀成功
                return miaoshaOrder.getOrderId();
            } else {
                // 数据库查不到订单原因 1. 秒杀失败 2. 在 rabbitmq 队列中，排队中，还没执行到   如何区分？
                // ajax 发起里查询秒杀结果的请求，说明秒杀请求已经提交到rabbitmq队列，如果还有此商品的库存，则应该是在排队中，反之秒杀失败
                boolean isOver = getGoodsOver(goodsId);     // true 表示没库存了即秒杀失败
                if (isOver) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    ```

## 总结5：点击秒杀按钮前，需要图形验证码才可进行秒杀实现
1. 考虑的问题：添加一个生成验证码的接口；在获取秒杀路径时验证验证码；ScriptEngine 使用
2.前端商品详情页如果正在秒杀显示验证码图片：
    ```
    else if (remainSeconds == 0) {   // 2. 正在进行秒杀：按钮able + 显示验证码
                $("#buyButton").attr("disabled", false);
                if (timeout) {
                    clearTimeout(timeout);
                }
                $("#miaoshaTip").html("秒杀进行中");
    
                <!-- 如果秒杀已开始，展示验证码图片及验证码输入框 -->
                <!-- 图片地址是 请求 "/miaosha/verifyCode?goodsId=" + $("#goodsId").val() 返回的图片 -->
    
    
                $("#verifyCodeImg").attr("src", "/miaosha/verifyCodeImage?goodsId=" + $("#goodsId").val());
                $("#verifyCodeImg").show();
                $("#verifyCode").show();
    ```
3. 服务端处理 /miaosha/verifyCode?goodsId=? 请求
    ```
        /**
         * BufferedImage ，代表着有数学表达式的验证码图片，通过 HttpServletResponse 的 outputStream 返回到客户端
         */
        @RequestMapping(value = "/verifyCodeImage", method = RequestMethod.GET)
        @ResponseBody
        public Result<String> getMiaoshaVerifyCodeImage(MiaoshaUser miaoshaUser,
                                                        @RequestParam("goodsId") long goodsId,
                                                        HttpServletResponse response) {
            logger.info("正在进行验证码验证");
            if (miaoshaUser == null) {
                return Result.error(CodeMsg.SESSION_ERROR);
            }
            // 生成验证码图片：将一个数学表达式写在验证码图片上，同时将计算结果缓存到 redis，返回这个图片
            // miaoshaUser, goodsId是用来作为验证码答案存在 redis 的键
            // 注意这里使用的是 response 的 outputStream 将这个图片返回到客户端的，所以 return null
            try {
                BufferedImage image = miaoshaService.createVerifyCodeImage(miaoshaUser, goodsId);
                OutputStream out = response.getOutputStream();
                ImageIO.write(image, "JPEG", out);
                return null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return Result.error(CodeMsg.MIAOSHA_FAIL);
            }
        }
    ```
    - MiaoshaService 生成验证码图片：将一个数学表达式写在验证码图片上，同时将计算结果缓存到 redis，返回这个图片
    ```
    // 生成验证码图片：将一个数学表达式写在验证码图片上，同时将计算结果缓存到 redis，返回这个图片
    public BufferedImage createVerifyCodeImage(MiaoshaUser miaoshaUser, long goodsId) {
            if (miaoshaUser == null || goodsId <= 0) {
                return null;
            }
            int width = 90;
            int height = 32;
            // 创建 BufferedImage 对象：内存里的图像
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics graphics = image.getGraphics();    // Graphics 看成画笔
            // 设定背景颜色：以 0xDCDCDC 颜色填充
            graphics.setColor(new Color(0xDCDCDC));
            graphics.fillRect(0, 0, width, height);
            // 以黑色画个矩形框
            graphics.setColor(Color.black);
            graphics.drawRect(0, 0, width - 1, height - 1);
            // 50 个随机干扰点
            Random rdm = new Random();
            for (int i = 0; i < 50; i++) {
                int x = rdm.nextInt(width);
                int y = rdm.nextInt(height);
                graphics.drawOval(x, y, 0, 0);
            }
            // 生成 数学公式 的字符串
            String verifyCode = createVerifyCode(rdm);
            graphics.setColor(new Color(0, 100, 0));                 // 画笔颜色
            graphics.setFont(new Font("Candara", Font.BOLD, 24));  // 画笔字体
            graphics.drawString(verifyCode, 8, 24);
            graphics.dispose();
    
            // 数学公式 的字符串的计算结果缓存到 redis
            int answer = calc(verifyCode);
            redisService.set(MiaoshaKey.getMiaoshaVerifyCode, miaoshaUser.getId() + "_" + goodsId, answer);
    
            // 输出图片
            return image;
        }
    
        // 生成 数学公式 的字符串
        private String createVerifyCode(Random rdm) {
            int number1 = rdm.nextInt(10);    // [0, 10) 之间的随机数
            int number2 = rdm.nextInt(10);
            int number3 = rdm.nextInt(10);
            char ops1 = ops[rdm.nextInt(3)];   // 没有除法是为了防止除以0异常，简化代码
            char ops2 = ops[rdm.nextInt(3)];
            return "" + number1 + ops1 + number2 + ops2 + number3;
        }
    
        // 计算数学表达式字符串的结果
        public int calc(String exp) {
            try {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("JavaScript");
                return (Integer) engine.eval(exp);
            } catch (ScriptException e) {
                e.printStackTrace();
                return 0;
            }
        }
    ```
4. 点击秒杀按钮首先验证验证码是否正确，这个请求进行了限流(总结3)，验证码正确就会生成秒杀地址给客户端，客户端利用ajax请求执行秒杀，服务端会
先验证地址是否正确，redis 预减库存是否还有库存，查询订单验证是否秒杀过了，然后将秒杀信息放入队列异步下单；从队列取出秒杀信息减库存+生成订单。 
    

# 五、开发过程
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

### 3.3 秒杀功能及订单详情页
1. 在商品详情页点击秒杀按钮即将 user 和 goodsId 传入 OrderController 进行处理
    - 编写 MiaoshaService 处理 (1)减库存 -> (2)下订单 -> (3)数据库插入秒杀订单 步骤，需要事务支持
    - 如果秒杀成功则进入 订单详情页 order_detail.html 如果失败则进入 miaosha_fail.html
2. OrderDao -> OrderService -> OrderController miaosha_fail.html order_detail.html
3. miaoshaService.miaosha(miaoshaUser, goods);     进行秒杀：(1)减库存 -> (2)生成订单 -> (3)数据库插入秒杀订单  这三个步骤需要 事务管理
4. 一般提倡在自己的Service下引入自己的Dao(比如说在GoodsService引入GoodsDao)，某Service下想使用其他的dao则引入对应的Service解决，
   比如MiaoshaService引入goodsService和orderService，因为service下可能会涉及到缓存的更新，直接调用dao则会忽略这点
5. 订单详情页 order_detail.html

## 4. JMeter 压测
### 4.1 JMeter 入门
1. 压测 /goods/to_list 页面  yace1.jmx
    - TestPlan->Add->Threads->Thread Group
    - Thread Group 下的 Number of Threads 就是并发数，这里先设为1000；Ramp-up Period 是通过多久启动全部线程，这里设为0 Loop count : 10
    - Thread Group 新建 Configure Element -> Http request default 这里配置好后，其他的请求就不需要再配了
    - Thread Group 新建 Sample -> Http Request
    - Thread Group 新建 Listener -> Aggregate report 和 Graph result
2. 结果显示 Throughput 大约在 2880/sec 左右
    ```
        @RequestMapping(value = "/to_list")
        public String list(Model model, MiaoshaUser miaoshaUser) {
            model.addAttribute("user", miaoshaUser);
    
            // 查询商品列表
            List<GoodsVO> goodsList = goodsService.listGoodsVO();
    
            model.addAttribute("goodsList", goodsList);
            return "goods_list";
        }
    ```
    - 可见性能的瓶颈在从mysql查询List<GoodsVO> goodsList = goodsService.listGoodsVO();

### 4.2 自定义变量模拟多用户
1. 压测 /user/info 页面,这个需要在请求参数里设置 token=db762a1e7fbc4857b7787e02f4e1ca09(在页面请求的请求cookie里查看)   yace2.jmx
2. 结果显示 Throughput 大约在 7000/sec 左右
    ```
        @RequestMapping(value = "/info")
        @ResponseBody
        public Result<MiaoshaUser> list(Model model, MiaoshaUser miaoshaUser) {
            model.addAttribute("user", miaoshaUser);
            return Result.success(miaoshaUser);
        }
    ```
3. 这里的QPS高的原因是因为只读了redis中的缓存获取用户信息，而商品列表页面不仅读了缓存还进行Mysql数据库的查询
    - **并发的瓶颈显然在数据库 mysql，数据库分库分表需要从一开始设计，否则很难扩展，这方面学习 Mycat**
4. 测试缺点：相同的 token，意味着均是同一个用户进行页面请求，如何模拟多用户？
    - Add -> configure element -> CSV Data Set Config  上传配置，里面是  userId,userToken
5. Recycle on EOF : 例如1000此请求，但只有10个token用户，是否允许到了token末尾后循环再次请求，设为 true

### 4.3 redis 压测
1. redis-benchmark -h 127.0.0.1 -p 6379 -c 100 -n 100000
     - -n 100000 -c 100 100个并发 100000个请求 -h host -p port   测试结果选取片段如下
     ```
    ====== GET ======
      100000 requests completed in 1.17 seconds
      100 parallel clients
      3 bytes payload
      keep alive: 1
    
    99.86% <= 1 milliseconds
    100.00% <= 1 milliseconds
    85763.29 requests per second
    ```
    - 这个结果是对 redis 的 get 进行压测的结果 ： QPS 85763；    3字节为单位进行测试的    
2. redis-benchmark -h 127.0.0.1 -p 6379 -q -d 100
    - -d 100 指以100字节为单位进行测试
    - -q 使得输出结果简洁                  测试结果选取片段如下
    ```
    PING_INLINE: 87260.03 requests per second
    PING_BULK: 86430.43 requests per second
    SET: 84961.77 requests per second
    GET: 85763.29 requests per second
    INCR: 84817.64 requests per second
    ```
3. redis-benchmark -t set,lpush -n 100000 -q
    - -t set,lpush 指压测 set push 命令
4. redis-benchmark -n 100000 -q script load "redis.call('set','foo','bar')"
    - script load "redis.call('set','foo','bar')"   只测试这条命令 redis.call('set','foo','bar') 

### 4.4 spring boot 打 war 包
1. spring boot 默认打 jar 包，如何生成 war 包？war 包可放在 tomcat 下运行
    - sprint-boot-starter-tomcat 加上 <provided> 类似于SSM架构下 javax.servlet-api   jsp-api  包：因为运行时是有 tomcat 的，编译以来，运行时是没有的
    - 添加 maven-war-plugin 插件
    - <packaging>war</packaging>
    - 修改启动类 继承一个类
    ```java
    public class MiaoshaApplication extends SpringBootServletIniializer {
       public static void main(String[] args) {
           SpringApplication.run(MiaoshaApplication.class, args);   
       }
       
       @Override
       protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {   
           return builder.sources(MiaoshaApplication.class);
       }
    }
    ```
2. 在项目路径下执行 mvn clean package 在target文件下的war包放入tomcat下的ROOT文件夹下启动tomcat即可

### 4.5 Jmeter 命令行压测 
1. /goods/to_list 页面    yace3.jmx
    - 命令行启动程序-linux 服务器上这样用 mvn clean package 编译项目会形成 jar 包
    - 进入target目录下出现一个.jar的文件，使用java命令 nohup java -jar miaosha.jar & 启动，这时程序已经运行
    - tail -f nohup.out 实时查看日志
2. Jmeter 配置：miaoshaYace.jmx
    - Number of Threads 5000；Ramp-up Period 0; Loop count : 10   5000个并发循环10次
    - 进入 yace3.jmx 所在文件夹执行 jmeter -n -t yace3.jmx -l result.jtl
    - rm -rf result.jtl 在执行一次为准
    - 打开 jemter 在结果报告中打开此文件即可浏览结果
    
### 4.6 综合应用 压测秒杀页面
1. /miaosha/do_miaosha 页面   yace4.jmx
    ```
    @RequestMapping(value = "do_miaosha")
    public String miaosha(MiaoshaUser miaoshaUser, Model model, @RequestParam("goodsId") long goodsId) {
            logger.info("MiaoshaController 正在处理 /miaosha/do_miaoha 请求......   goodsId: " + goodsId);
            model.addAttribute("user", miaoshaUser);
            ...
            ...
            return "order_detail";   
        }
    ```
    - 显然需要参数 token 和 goodsId
2. 生成5000个用户插入到数据库中，并根据 id和密码 进行登陆(/login/do_login)，使得服务端生成token缓存到redis中，并且获取这些token到tokens.txt文件里
    - do_login 页面 controller 返回的是 Result<Boolean>，为了获取 token，需要对 controller 进行改造
        ```
        @RequestMapping(value = "/do_login")
        @ResponseBody
        public Result<Boolean> doLogin(HttpServletResponse response, @Valid LoginVO loginVO) {
                miaoshaUserService.login(response, loginVO);
                return Result.success(true);
            }
        ```
        ```
        @RequestMapping(value = "/do_login")
        @ResponseBody
        public Result<String> doLogin(HttpServletResponse response, @Valid LoginVO loginVO) {
                String token = miaoshaUserService.login(response, loginVO);
                return Result.success(token);
            }
        ```
3. 压测： jmeter -n -t yace4.jmx -l result.jtl 命令行压测 或者 直接在本地运行程序   
    - 1000线程用户，循环10次，线程数太大本地会溢出。
    - goodsId=2的商品秒杀库存为500
    - 结果：QPS：1557.9/s  卖了631份，卖超131
    ![秒杀用户token](./jmeter/pics/秒杀用户token.png "秒杀用户token")
    ![秒杀线程参数](./jmeter/pics/秒杀线程参数.png "秒杀线程参数")
    ![秒杀请求截图](./jmeter/pics/秒杀请求截图.png "秒杀请求截图")
    ![秒杀压测结果](./jmeter/pics/秒杀压测结果.png "秒杀压测结果")
    
## 5. 页面优化
### 5.1 页面缓存+URL缓存+对象缓存
1. 并发的瓶颈在数据库，使用各种粒度的缓存减少对数据库的访问
2. 页面缓存：访问页面不是直接由系统渲染，而是首先从缓存里面取，如果找到直接返回给客户端，没有则手动渲染这个模版，渲染后再将结果输出给客
   户端，同时将结果缓存到redis中     以 /goods/to_list 页面为例
   - 取缓存
   - 手动渲染模版
   - 结果输出
3. URL 级缓存：和页面缓存几乎一样，区别是页面缓存是没有参数的，大家访问的均是同一个页面，但/goods/to_list 对于不同的商品返回不同的页面，
   有个 goodsId 参数区分。例子是 /goods/to_detail/{goodsId}
4. 对象级缓存：最小粒度的缓存，涉及到数据更新同时也需要更新缓存
    - 分布式session中每个token对应一个对象，这是最小粒度的缓存(miaoshaUserService中实现 getByToken 方法(根据 token 获取对象))；
    - 将miaoshaUserService中 getById 方法也改造成对象级缓存(根据 id 获取对象)
    - 将miaoshaUserService中 updatePassword 方法也改造成对象级缓存(更新密码) 
    - 对象级缓存一定要注意如果有数据完成了更新，把缓存也要更新掉，否则会出现数据不一致，这是和页面缓存最大的区别
    - 自己的Service下调用自己的dao，如 MiaoshaUserService 调用 MiaoshaUserDao，其他的 service 想使用这个 MiaoshaUserDao，
    需调用 MiaoshaUserService，因为 MiaoshaUserService 涉及到缓存的
5. 这里的页面级缓存URL 级缓存在controller实现，对象级缓存在service层实现
6. 压测进行过页面缓存的 /goods/to_list 页面，对比前后 QPS 变化

### 5.2 页面静态化
1. 把页面缓存到浏览器上，Vue.js Angular.js 等前端技术，页面只存html，动态数据通过接口从服务端获取
    - 页面静态化本质上就是 静态html + ajax + controller 返回 json
    - 静态html一些事件触发 ajax 请求到服务端，返回 json，利用 DOM 修改页面
2. 页面静态化改造商品详情页 goods/detail/{goodsId}
    - 使用页面静态化之前在 goods_list 页面中点击<a th:href="'/goods/to_detail/' + ${goods.id}">链接请求这个页面，结合配置文件会
    去 /templates/ 文件夹下找以 .html 结尾的文件；将其改成 <a th:href="'/goods_detail.htm?goodsId=' + ${goods.id}"> 这样就会跳
    转到 /static/goods_detail.html 静态页面。然后静态页面利用 ajax 发出请求 /goods/detail/{goodsId} 请求，在GoodsController 获取请
    求返回 json 结果，最后利用js将 json 结果传递到页面
    - 建一个 GoodsDetailVO 向页面中传值，作为controller返回的json数据:观察  /goods/detail/{goodsId} 页面 controller 就可以发现向 model 中
    传的值是 GoodsVO goods + int miaoshaStatus + int remainSeconds + MiaoshaUser miaoshaUser；所以将这 4 个作为 GoodsDetailVO 的属性
3. 页面静态化改造秒杀
    - 使用页面静态化之前在 goods_detail 页面点击秒杀表单向 /miaosha/do_miaosha 发送请求，传递一个 商品id，返回渲染后的 order_detail 页面
    - 页面静态化 在 goods_detail 页面点击秒杀触发点击事件，利用ajax向 /miaosha/do_miaosha 发送请求获取返回的 json 对象 Result<OrderInfo>，
    跳转到订单详情页。
4. 页面静态化使得再次向服务端请求这些页面时，浏览器会给服务端传递一个if-Modified-since参数，服务端会检查页面是否发生变化，如果没有变化(静态页面无变化)，就
   返回304(304状态码表示客户端已经执行了GET，但文件未变化)，浏览器就可以直接使用本地缓存数据，只需要请求 json 对象。但是这里依然有询问服务端页面是否有变化。
5. 使用 spring 静态文件配置让浏览器不再询问服务端，直接从缓存取数据。然后请求 json 对象
    - spring.resources.cache.period=3600s 配置表示缓存存在时间3600s，直接返回页面200状态码
    - 会发现请求头有 Cache-Control:max-age=3600   指定缓存多久
    - 浏览器请求头：Cache-Control:max-age<=0表示每次请求都会访问服务器并通过Last-Modified确定文件是否已修改，若已修改，返回最新文件，
    否则返回304读取缓存文件。Cache-Control:max-age>0表示直接读取缓存。Cache-Control:no cache表示总是请求服务器最新文件，无304。 
6. 页面静态化改造订单详情页
    - 页面静态化之前，在商品详情页点击秒杀按钮，向服务端POST发送商品id表单，成功后返回orderInfo,goodsVO渲染order_detail页面
    - 静态化之后，在商品详情页点击秒杀按钮触发ajax，向miaosha/do_miaosha发送秒杀请求，成功(秒杀成功意味着减库存->生成订单->数据库插入秒杀订单)
    后跳转到订单详情静态页面并有请求参数?orderId=...。商品详情页ajax发起 /order/detail 请求，得到 Result<OrderDetailVO>，然后修改 order_detail 页面
7. 页面静态化就是 (1)先打开一个静态页面 (2)利用ajax想服务端发送请求，获取页面所需数据 (3)服务端返回页面所需数据，可能需要创建页面所需数据的
   对象，例如 GoodsDetailVO，OrderDatailVO 那么服务端返回 json 数据 Result<GoodsDetailVO> (4)js 利用返回的json数据修改页面

### 5.3 卖超问题和一个用户秒杀多个商品
1. 卖超问题：在使用 jmeter 压测时发现秒杀商品卖的比存货数量大
    ```
        @RequestMapping(value = "do_miaosha", method = RequestMethod.POST)
        @ResponseBody
        public Result<OrderInfo> miaosha(MiaoshaUser miaoshaUser, Model model, @RequestParam("goodsId") long goodsId) {
            logger.info("MiaoshaController 正在处理 /miaosha/do_miaoha 请求......   goodsId: " + goodsId);
            // 1. 限制条件，如果用户没登陆，反复登陆页面
            if (miaoshaUser == null) {
                return Result.error(CodeMsg.SESSION_ERROR);
            }
            // 2. 判断是否有库存：如果此 goodsId 对应商品没有库存
            GoodsVO goods = goodsService.getGoodsVOByGoodsId(goodsId);                                                // GoodsVO good
            int stockCount = goods.getStockCount();
            if (stockCount <= 0) {
                return Result.error(CodeMsg.MIAOSHA_OVER);
            }
            // 3. 判断是否已经秒杀：如果已经秒杀过了
            MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);   // MiaoshaUser user
            if (miaoshaOrder != null) {
                model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
                return Result.error(CodeMsg.REPEATE_MIAOSHA);
            }
            // 4. 进行秒杀：(1)减库存 -> (2)生成订单 -> (3)数据库插入秒杀订单  这三个步骤需要 事务管理
            OrderInfo orderInfo = miaoshaService.miaosha(miaoshaUser, goods);
            return Result.success(orderInfo);
        }
    ```
    ```
    public class MiaoshaService {
        @Autowired
        private GoodsService goodsService;
    
        @Autowired
        private OrderService orderService;
    
        // 进行秒杀：(1)减库存 -> (2)数据库插入生成的秒杀订单与订单
        @Transactional
        public OrderInfo miaosha(MiaoshaUser user, GoodsVO goods) {
            // (1)减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
            goodsService.reduceStock(goods);
            // (2)生成订单:即在 order_info 和 miaosha_order 表中插入一条记录
            // miaosha_order 是 order_info 子集，只包含参加秒杀活动的商品订单
            return orderService.createOrder(user, goods);
        }
    }
    ```
    ```
    public interface GoodsDao {
        // 更新:减库存，更新 miaosha_user 表中的 stockCount
        @Update("update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId}")
        int reduceStock(MiaoshaGoods mg);
    }
    ```
    - 秒杀需要先判断是否有库存，当库存数量为1时，如果有多个用户发起秒杀请求，此时有库存，这些用户均进入 MiaoshaService 的 
    miaosha(MiaoshaUser user, GoodsVO goods) 方法，GoodsDao 的 reduceStock 方法，执行 sql 语句。结果就会使得卖超。
    - 解决方法：update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId} and stock_count > 0
2. 单个用户秒杀多个商品：当库存多于 2 个时。同一个用户同时发起两次秒杀请求，就会出现这种情况
    - 利用数据库的唯一索引，在插入生成的 秒杀订单(miaosha_order不是order_info，后者订单是可以单个用户多次购买商品的) 时利用 
    商品id 和 用户 只能存在一个的条件插入不了第二条记录，由于 事务管理 就会回滚减库存操作
    - 解决方法：在 miaosha_order 表上建立一个唯一索引 UNIQUE KEY `u_uid_gid` (`user_id`,`goods_id`)
3. 缓存下秒杀订单，在 OrderService 下的 createOrder 里生成 订单和秒杀订单，缓存到redis中，当在getMiaoshaOrderByUserIdGoodsId方法
   中取就可以不用访问数据库了。可以发现 Controller 里有 redisService 属性的用于页面缓存，Service 类里有 redisService 属性的用于对象缓存
4. 压测下 是否还出现卖超等问题     jmeter -n -t yace4.jmx -l result.jtl 命令行压测 或者 直接在本地运行程序 注意是 post 请求
    - 1000线程用户，循环10次，线程数太大本地会溢出。
    - goodsId=2的商品秒杀库存为500
    - 秒杀压测前需要清空redis和mysql中数据 flushdb；然后使用 UserUtil 命令先登录这1000用户；在进行压测。这里多了个清理redis，是因为
    订单缓存到了redis中
    - 结果：QPS：3103.7/s  卖了544份，虽然 miaosha_goods 的库存并没有变负数，是0，但是 order_info 的订单数为 544。
    - 原因：如果因为库存不够，减库存失败，但是依然会生成订单。所以需要略微修改下减库存代码
    ```
    @Service
    public class MiaoshaService {
        @Autowired
        private GoodsService goodsService;
        @Autowired
        private OrderService orderService;
        // 进行秒杀：(1)减库存 -> (2)数据库插入生成的秒杀订单与订单
        @Transactional
        public OrderInfo miaosha(MiaoshaUser user, GoodsVO goods) {
            // (1)减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
            goodsService.reduceStock(goods);
            // (2)数据库插入生成的秒杀订单与订单:即在 order_info 和 miaosha_order 表中插入一条记录
            // miaosha_order 是 order_info 子集，只包含参加秒杀活动的商品订单
            return orderService.createOrder(user, goods);
        }
    }
    ```
    
    ```
    @Service
    public class GoodsService {
        ...
        @Autowired
        private GoodsDao goodsDao;
        // 减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
        public void reduceStock(GoodsVO goods) {
            MiaoshaGoods mg = new MiaoshaGoods();
            mg.setGoodsId(goods.getId());     // miaoshaGoods 的商品id 是 goodsId
            goodsDao.reduceStock(mg);
        }
    }
    ```
    修改为
    ```
        @Transactional
        public OrderInfo miaosha(MiaoshaUser user, GoodsVO goods) {
            // (1)减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
            // 虽然 MiaoshaController 里已经在 MiaoshaController 里检查了是否有库存，但可能发生多个请求同时检查库存，例如库存为2，同时有10个请求，
            // 这时有库存，但实际数据库操作就会减库存失败8次，因此这8次不可以生成订单，因为库存没了
            boolean success = goodsService.reduceStock(goods);
            // (2)数据库插入生成的秒杀订单与订单:即在 order_info 和 miaosha_order 表中插入一条记录
            // miaosha_order 是 order_info 子集，只包含参加秒杀活动的商品订单
            if (success) {
                return orderService.createOrder(user, goods);
            } else {
                // 库存没了，标记该商品秒杀卖完了：在redis中插入 goodsId -> true(商品卖完)
                // 重要，当客户端请求秒杀结果时，用于区分是在排队还是秒杀失败
                setGoodsOver(goods.getId());
                return null;    // throw new GlobalException(CodeMsg.MIAOSHA_OVER)
            }
        }
    ```
    ```
    @Service
    public class GoodsService {
        ...
        // 减库存：在 miaosha_goods 表中更新 stockCount 的值，减1
        public boolean reduceStock(GoodsVO goods) {
            MiaoshaGoods mg = new MiaoshaGoods();
            mg.setGoodsId(goods.getId());
            return goodsDao.reduceStock(mg) > 0;  // false表明减库存失败，这时就不应该生成订单了
        }
    
    }
    ```
    
5. 后面重点优化 秒杀

## 6. 接口优化
### 6.1 rabbitmq集成
1. 安装好rabbitmq后加入环境变量 ~/.zshrc 文件 export PATH=$PATH:/usr/local/sbin
    - 启动：rabbitmq-server
    - 停止：rabbitmqctl stop
    - web管理页面 http://localhost:15672/ 登录guest 密码guest
2. spring boot 集成 rabbitmq
    - 添加 spring-boot-starter-amqp 依赖，进行配置，定义一个 Queue (指的是 MQConfig 中的 @Bean)
    - 创建消息接收者
    - 创建消息发送者
3. 4 种交换机模式 direct topic fanout headers

### 6.2 redis 预减库存 + rabbitmq 异步下单
1. 原先的秒杀流程
    - (1)判断用户是否登陆，没有返回 Result.error(CodeMsg.SESSION_ERROR) (2)判断库存，没有返回Result.error(CodeMsg.CodeMsg.MIAOSHA_OVER)
    (3)判断该用户是否已经秒杀过了 (4)进行秒杀 (4-1)减库存 (4-2)减库存成功，再生成订单
    - 注意(4-1)减库存是可能失败的，因为在 MiaoshaController 里如果只有 2 个库存，但是同时有10个请求，此时判断库存是有库存的，所以这10个请求都会进
    入秒杀步骤，所以减库存成功的话再生成订单
2. 思路：redis 预减库存使得减少对数据库的访问，rabbitmq 使得原先同步下单改成了异步下单
    - (1)系统初始化，把 商品id-商品库存数量 加载到 redis：看 public class MiaoshaController implements InitializingBean 类
    - (2)收到秒杀请求，redis 预减库存，库存不足，直接返回，否则继续
    - (3)秒杀请求压入 rabbitmq 队列，立即返回排队中(无阻塞)，这样客户端就会立马收到响应:所以(4)(5)是并发进行的
    - (4)服务端秒杀请求出队，生成订单，会把订单写到缓存里，减少库存
    - (5)客户端发起查询秒杀结果请求，是否秒杀成功，如果是排队中则再次请求
3. 进一步优化减少对 redis 的请求
    - 秒杀过程中，服务端的请求首先在 redis 中查看所秒杀商品的库存，如果库存 stock < 0，直接返回就不用去访问数据库了，问题上商品库存没了，
    客户端依然会大量请求redis
    - 在 MiaoController 里加上个本地标识 goodsID -> true/false ： 商品 -> 此商品秒杀是否结束了
    ```
    @RequestMapping(value = "/do_miaosha", method = RequestMethod.POST)
        @ResponseBody
        public Result<Integer> miaosha(MiaoshaUser miaoshaUser, @RequestParam("goodsId") long goodsId) {
            logger.info("用户 " + miaoshaUser.getId() + " 正在秒杀，秒杀商品的id是 " + goodsId);
    
            if (miaoshaUser == null) {
                return Result.error(CodeMsg.SESSION_ERROR);
            }
            // 2. 收到请求，redis 预减库存，库存不足，直接返回，否则继续；返回的是剩下的库存
            // 可能会出现单个用户同时发起多个请求，减库存了，但是真正生成订单的只有1个，这可以通过验证码防止，另外卖超是不允许的，卖不完是允许的
            long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
            if (stock < 0) {
                return Result.error(CodeMsg.MIAOSHA_OVER);
            }
            // 判断是否秒杀过了：根据 userId 和 goodsId 查询是否有订单存在
            MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
            if (miaoshaOrder != null) {
                return Result.error(CodeMsg.REPEATE_MIAOSHA);
            }
            // 3. 秒杀请求压入 rabbitmq 队列，立即返回排队中(无阻塞)
            MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
            miaoshaMessage.setGoodsId(goodsId);
            miaoshaMessage.setMiaoshaUser(miaoshaUser);
            mqSender.sendMiaoshaMessage(miaoshaMessage);
            return Result.success(0);   // 秒杀请求压入 rabbitmq 队列，立即返回，无阻塞
        }
    ```
4. 注意到Service层中 MQReceiver 中的 receiveMiaoshaQueue 方法 和 miaoshaService 中的 miaosha 方法均没有抛出异常进行处理。
    - 之前的 Service 中方法抛出异常，这是因为这些Service中方法在controller里使用了，异常会传递到controller层。统一异常处理使用
     @ControllerAdvice + @ExceptionHandler 进行全局的 Controller 层异常处理，这样这些异常就会捕获，异常捕获处理返回的类型和
     controller层一样均是Result类型。
    - MQReceiver 这个Service层的类并没有被controller中类引用。因为异步使用它的。miaoshaService 中的 miaosha 方法也是在 MQReceiver
    中被调用。
5. jmeter 压测            jmeter -n -t yace4.jmx -l result.jtl 命令行压测 或者 直接在本地运行程序 注意是 post 请求
    - 1000线程用户，循环10次，线程数太大本地会溢出。goodsId=2的商品秒杀库存为500
    - 秒杀压测前还要清空 mysql 的订单数据 和 redis的全部缓存 flushdb；将库存调为 500
    - 然后启动 springboot 系统启动时就把商品库存数量加载到 redis:每个秒杀商品id是键，对应商品的库存是值 同时利用 localOverMap 在本
    地标记每个商品有库存，可秒杀的
    - 接着使用 UserUtil 命令先登录这1000用户，在redis 中缓存这1000用户的 token
    - 进行压测
    - 结果：QPS：3518.6/s  卖了 499 份，miaosha_goods 的库存并没有变负数，是 1。推测是因为同一时刻某个用户发起多个请求，后面的请求无法秒杀
    但是redis的库存已经减了，所以有1个没卖出去。
    - 如何解决？(1)卖超是不允许的，但是卖不完是允许的。(2)初始化的时候 商品数量比库存多一点就好了
    
## 6. 安全优化
### 6.1 秒杀地址隐藏
1. 改造前：在 goods_detail.htm 页面点击秒杀按钮，利用ajax向 "miaosha/do_miaosha" 发起请求，将秒杀信息入队，进行秒杀
2. 改造后：在 goods_detail.htm 页面点击秒杀按钮先获取秒杀地址，即利用 ajax 先向 miaosha/path 发起请求，得到随机字符串，将这个字符串发送给客户端
作为秒杀地址，同时暂时缓存到redis中。紧接着客户端请求 /miaosha/{path}/do_miaosha ，和 redis 中对比 path ，如果正确将秒杀信息入队，进行秒杀
3. 优势：改造前客户可以通过客户端查看前端代码直接请求 miaosha/do_miaosha 并传递 goodsID ；改造后无法获取秒杀地址只能通过点击秒杀按钮先获取秒杀地址

### 6.2 数学公式验证码
1. 改造前：在 goods_list 页面点击商品详情，进入 /goods_detail.htm?goodsId= 商品详情页静态页面。商品详情页立即使用 ajax 请求
 "/goods/to_detail/" + goodsId 获取此页面需要的 json 数据，然后将页面渲染出来。页面渲染出来后进行倒计时：(1) 显示倒计时 + 秒杀按钮disable
(2) 显示秒杀已开始 + 按钮able (3) 秒杀已结束 + 按钮disable
2. 改造后：前面请求一样，只是页面渲染出来后进行倒计时：根据 remainSeconds 的值决定显示效果：(1) 显示倒计时 + 秒杀按钮disable + 不显示验证码
(2) 显示秒杀已开始 + 按钮able + 显示验证码 (3) 秒杀已结束 + 按钮disable + 不显示验证码；这些验证码图片的 src 属性是
"/miaosha/verifyCodeImage?goodsId=" + $("#goodsId").val()) ，对这个url的请求会返回一个验证码图片，同时将答案展示缓存到redis中。当在
验证码输入框输入验证值后点击秒杀按钮会同时将这个结果作为请求参数，和redis中缓存的进行对比。正确得啊则请求到秒杀地址，然后秒杀。

### 6.3 接口限流防刷(自定义注解+拦截器)
    - 见重点小结：如何实现接口限流防刷，防止用户使用程序短时间内多次请求某个接口，例如短时间大量进行秒杀请求？

## 7. 服务器优化
### 7.1 Tomcat 优化
0. 一些Tomcat参数可能会影响性能，在文档里查看然后可根据实际情况进行配置
1. 内存优化 catalina
    - 本机的Tomcat安装在 ～/Library/Tomcat 目录 ${tomcat}=～/Library/Tomcat
    - 位置 ${tomcat}/bin/catalina.sh 添加一下语句
    ```
    # Tomcat 内存优化:最小内存最大内存配为2g；当出现内存溢出时把内存影像dump出来放到$CATALINA_HOME/logs/heap.dump
    JAVA_OPTS="-server -Xms2048M -Xmx2048M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$CATALINA_HOME/logs/heap.dump"
    ```
2. 并发优化
    - 参考 ${tomcat}/webapps/docs/config/http.html 文档
    - 几个参数了解
    - maxConnections：The maximum number of connections that the server will accept and process at any given time. 
    When this number has been reached, the server will accept, but not process。For NIO and NIO2 the default is 10000. 
    For APR/native, the default is 8192.
    - acceptCount：The maximum queue length for incoming connection requests when all possible request processing threads are in use.
    Any requests received when the queue is full will be refused. The default value is 100.
    - maxThreads：工作线程
    - minSpareThreads：最小空闲连接
    - 对以上参数进行配置，配置位置 ${tomcat}/conf/server.xml
    ```
    <!--maxConnections以下是Tomcat的并发优化，默认值及意义见${tomcat}/webapps/docs/config/http.html 文档-->
    <Connector port="8080" protocol="HTTP/1.1"
                   connectionTimeout="20000"
                   redirectPort="8443" 
                   maxConnections="300"
                   acceptCount="200"
                   maxThreads="400"
                   minSoareThreads="200"
    />
    ```
    
3. 其他优化
    - 参考 ${tomcat}/webapps/docs/config/host.html 文档和 ${tomcat}/webapps/docs/config/http.html 文档
    - 几个参数了解
    - autoDeploy：(host.html) This flag value indicates if Tomcat should check periodically for new or updated web applications 
    while Tomcat is running. If true, Tomcat periodically checks the appBase and xmlBase directories and deploys any
    new web applications 
    - enableLookups： (http.html) Set to true if you want calls to request.getRemoteHost() to perform DNS lookups in order to 
    return the actual host name of the remote client.     enableLookups：false
    - reloadable：(context.html) Set to true if you want Catalina to monitor classes in /WEB-INF/classes/ and /WEB-INF/lib for changes, 
    and automatically reload the web application if a change is detected. 
    
4.  APR 优化
    - Tomcat支持许多 connector 最原始的是IO，阻塞式的；然后非阻塞的NIO； IO -> NIO -> NIO2 -> AIO -> APR 


### 7.2 Nginx 优化:如何配置nginx
1. 两个Tomcat + 一个nginx 负载均衡
2. 安装Nginx，配置nginx。我这里使用homebrew安装的，配置文件在/usr/local/etc/nginx/nginx.conf  
    - 常见命令
    ```
    启动Nginx: nginx
    快速停止或关闭Nginx：nginx -s stop
    正常停止或关闭Nginx：nginx -s quit
    配置文件修改重装载命令：nginx -s reload
    查看进程 ps -ef | grep nginx









