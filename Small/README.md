# Small —— Spring Boot 3.2.0 + Spring Security + MySQL 示例项目

一个最小可运行的后端项目，包含两个接口：

| 接口 | 方法 | 是否需认证 | 说明 |
| --- | --- | --- | --- |
| `/helloWorld` | GET | 需要 | 返回字符串 `HelloWorld` |
| `/login` | POST | 不需要 | 用户名密码登录，返回 JWT 令牌 |

测试账号：**test / 123456**；数据库：**MySQL，root / 123456**，数据库名 `hello_security`（首次启动自动创建）。

---

## 一、技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Spring Boot | 3.2.0 |
| Spring Security | 6.2（随 Boot 3.2.0 引入） |
| Spring Data JPA (Hibernate) | 随 Boot 3.2.0 引入，`ddl-auto=update` 自动建表 |
| MySQL | 8.0（驱动 `com.mysql:mysql-connector-j`） |
| JDK | 17 及以上（`pom.xml` 中 `java.version=17`） |
| 令牌 | 自实现 JWT（HMAC-SHA256，JDK 自带 `javax.crypto`，不引第三方库） |
| 构建 | Maven 3.6+ |

---

## 二、认证设计

1. **登录**：`POST /login` 提交 JSON 用户名密码 → 交给 Spring Security 的 `AuthenticationManager` 认证 → 内部由自定义 `UserDetailsService` 查 `sys_user` 表、用 BCrypt 校验密码 → 成功后签发 JWT。
2. **访问受保护接口**：请求头带 `Authorization: Bearer <token>` → `JwtAuthenticationFilter` 校验签名和过期时间 → 查库加载用户 → 放入 `SecurityContext` → 授权通过后返回 `HelloWorld`。
3. **无状态**：`SessionCreationPolicy.STATELESS`，不创建 HttpSession；关闭了 Spring Security 默认的表单登录、Basic 认证、CSRF、登出。
4. **未认证 / 无权限**：返回 JSON（401 / 403），不会跳转登录页。

```
POST /login  ──► AuthenticationManager ──► UserDetailsService ──► sys_user(BCrypt)
                    │ 认证通过
                    ▼
               JwtUtil 签发 token ──► 客户端保存
                    │
GET /helloWorld （带 Bearer token）──► JwtAuthenticationFilter 校验 ──► SecurityContext
                    ▼
               "HelloWorld"
```

---

## 三、目录结构

```
D:\AAAShiXi\Small
├── pom.xml
├── api.http                      # IntelliJ HTTP Client 请求示例（可直接点击运行）
├── README.md
├── .gitignore
└── src/main
    ├── java/com/example/small
    │   ├── SmallApplication.java              # 启动类
    │   ├── config/SecurityConfig.java         # Security 过滤链、密码编码器、UserDetailsService
    │   ├── controller/HelloController.java    # GET /helloWorld
    │   ├── controller/AuthController.java     # POST /login
    │   ├── dto/LoginRequest.java              # 登录请求体
    │   ├── dto/LoginResponse.java             # 登录响应体
    │   ├── entity/SysUser.java                # 用户实体
    │   ├── repository/UserRepository.java     # JPA 数据访问
    │   ├── security/JwtUtil.java              # 令牌生成与校验
    │   ├── security/JwtAuthenticationFilter.java  # 令牌认证过滤器
    │   └── init/DataInitializer.java          # 启动时初始化 test 用户
    └── resources
        ├── application.yml                    # 端口、数据源、JPA、JWT 配置
        └── static/index.html                  # 可视化控制台页面（登录 + 接口调用）
```

---

## 四、数据库

`application.yml` 中的连接串已带 `createDatabaseIfNotExist=true`，只要 MySQL 的 `root` 账号可用，首次启动会自动创建 `hello_security` 库；表由 Hibernate 自动创建。

启动后实际生成的表与数据：

```sql
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,   -- BCrypt 密文，不存明文
    nickname VARCHAR(50),
    enabled BIT NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY (username)
);
```

`DataInitializer` 在启动时检查 `test` 是否存在，不存在则插入 `test / 123456`（密码经 BCrypt 加密）。想换成自己的账号，直接改 `DataInitializer` 里的常量，或往表里插一条 BCrypt 密文即可。

数据源配置（`src/main/resources/application.yml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hello_security?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
    username: root
    password: 123456
```

---

## 五、运行

### 1. 准备 JDK 17+

Spring Boot 3.2 不支持 JDK 8。确认 `java -version` 是 17 或更高：

```powershell
java -version
```

本机 `JAVA_HOME` 当前指向 `D:\Program Files\Jdk 18`（实际是 JDK 1.8），直接运行 Maven 会失败。可用本机已有的 JDK 25：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

### 2. 启动 MySQL

确保 MySQL80 服务在运行（`Get-Service MySQL80`），且 root 密码为 `123456`。

### 3. 编译打包并运行

```powershell
# 在项目根目录 D:\AAAShiXi\Small 下
mvn clean package -DskipTests
java -jar target\small-0.0.1-SNAPSHOT.jar
```

启动成功的关键日志：

```
Tomcat started on port 8080 (http) with context path ''
Started SmallApplication in 3.124 seconds
已初始化测试用户：test / 123456
```

IDEA 中也可以直接运行 `SmallApplication` 的 `main` 方法，或使用 Spring Boot 运行配置。

### 4. 打开界面

浏览器访问 <http://localhost:8080/>（改了端口就用实际端口），可以看到登录与接口调用的可视化控制台。

---

## 六、可视化界面

启动应用后打开 <http://localhost:8080/>，页面上就能走完整条安全链路，不需要 Postman：

- **登录面板**：预填 `test / 123456`，点「登录并获取令牌」。成功后展示令牌（可复制）、剩余有效期倒计时（到点自动提示过期），以及「退出登录」。
- **接口面板**：「不带令牌调用」会看到真实的 401 JSON；「带令牌调用」返回 `HelloWorld`。每次请求显示 HTTP 状态码、耗时、响应类型和格式化后的响应体，下方保留最近 6 条请求记录。
- **底部三栏**：说明这条链路的三步——登录校验、签发 JWT、过滤器校验。

实现说明：

- 页面是**单文件** `src/main/resources/static/index.html`，CSS 和 JS 全部内联，没有前端构建步骤，改完直接刷新浏览器。
- 令牌存在浏览器 localStorage，刷新页面仍是登录态，过期自动清除。
- 为了让页面本身可公开访问，`SecurityConfig` 放行了静态资源，接口仍然需要认证：

```java
.requestMatchers("/", "/index.html", "/favicon.ico", "/favicon.svg").permitAll()
.requestMatchers("/css/**", "/js/**", "/assets/**", "/webjars/**").permitAll()
.requestMatchers(HttpMethod.GET, "/helloWorld").authenticated()
```

- 如果页面返回 401 或打不开，说明正在运行的还是改动前的实例（在 IDEA 里没有重启），Stop 后再 Run 一次即可。

---

## 七、接口测试

### 1. PowerShell / curl

```powershell
# ① 未登录访问受保护接口 → 401
Invoke-WebRequest -Uri http://localhost:8080/helloWorld -SkipHttpErrorCheck -UseBasicParsing

# ② 登录，拿 token
$body = @{username="test"; password="123456"} | ConvertTo-Json
$resp = Invoke-RestMethod -Uri http://localhost:8080/login -Method Post -Body $body -ContentType "application/json"
$token = $resp.token

# ③ 带 token 访问 → HelloWorld
Invoke-WebRequest -Uri http://localhost:8080/helloWorld -Headers @{Authorization="Bearer $token"} -SkipHttpErrorCheck -UseBasicParsing
```

```bash
curl -i http://localhost:8080/helloWorld
curl -i -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"username":"test","password":"123456"}'
curl -i http://localhost:8080/helloWorld -H "Authorization: Bearer <上一步返回的 token>"
```

### 2. IntelliJ HTTP Client

打开项目根目录的 `api.http`，依次执行即可（第 2 个请求会自动把 token 存入变量供第 3 个请求使用）。

### 3. 本项目实测结果（2026-11-14，JDK 25 运行）

| 场景 | 请求 | 结果 |
| --- | --- | --- |
| 未携带令牌 | `GET /helloWorld` | `401`，`{"code":401,"message":"未认证：请先调用 POST /login 获取令牌..."}` |
| 正确登录 | `POST /login`，`{"username":"test","password":"123456"}` | `200`，返回 `username`、`token`、`tokenType=Bearer`、`expiresIn=7200` |
| 携带令牌 | `GET /helloWorld` + `Bearer <token>` | `200`，响应体 `HelloWorld` |
| 密码错误 | `POST /login`，`{"username":"test","password":"wrong"}` | `401`，`{"code":401,"message":"用户名或密码错误"}` |
| 伪造令牌 | `GET /helloWorld` + `Bearer abc.def.ghi` | `401` |

界面同样实测过：`GET /` 返回 `200`（`text/html`），页面上登录、带令牌调用、不带令牌调用、401 提示、令牌倒计时与请求记录均正常。

---

## 八、常见问题

**1. Maven 下载依赖很慢或超时**

本机访问 Maven 中央仓库 `repo.maven.apache.org` 会超时，腾讯镜像可用。在 `C:\Users\<用户名>\.m2\settings.xml` 中加镜像（文件不存在就新建）：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>tencent-mirror</id>
      <mirrorOf>*</mirrorOf>
      <name>Tencent Maven Mirror</name>
      <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>
    </mirror>
  </mirrors>
</settings>
```

也可以用命令行临时指定：`mvn -s 你的settings.xml clean package`。

**2. 报 `Unsupported class file major version 55` / `javac` 编译失败**

Maven 用的 JDK 太老（JDK 8 的 class 版本是 52，Boot 3.x 需要 17+）。按"五、运行"第一小节切换 `JAVA_HOME`，IDEA 里把 `Project SDK` 和 `Maven → Runner → JRE` 也改成 17+。

**3. 启动报 `Access denied for user 'root'@'localhost'`**

确认 MySQL 服务已启动，root 密码是 `123456`；若密码不同，改 `application.yml` 的 `spring.datasource.password`。

**4. 启动报 `Communications link failure`**

MySQL 没启动或端口不是 3306，检查 `Get-Service MySQL80` 与连接串里的端口。

**5. 端口 8080 被占用**

改 `application.yml` 的 `server.port`；或先结束占用进程：`Get-NetTCPConnection -LocalPort 8080`。

**6. 401 但令牌是刚拿到的**

令牌有效期 120 分钟（`app.jwt.expire-minutes`），请求头必须是 `Authorization: Bearer xxx`（注意 Bearer 后有空格）；重启应用后如果改了 `app.jwt.secret`，旧令牌会全部失效。

---

## 九、生产环境注意事项

- `app.jwt.secret` 目前写在配置文件里，正式环境请改为环境变量注入（如 `APP_JWT_SECRET`），并换成长随机字符串。
- 数据库密码同理，不要硬编码在仓库里。
- 示例用 `ddl-auto=update` 方便开发，生产建议改为 `validate` 并用 Flyway/Liquibase 管理表结构。
- 如需角色/权限区分，可在 `UserDetailsService` 中按用户赋予不同 `roles`，再用 `@PreAuthorize` 或 `authorizeHttpRequests` 做细粒度控制。
