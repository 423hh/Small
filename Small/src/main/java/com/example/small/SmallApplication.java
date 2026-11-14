package com.example.small;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类。
 *
 * <p>启动后：
 * <ul>
 *   <li>GET /helloWorld：需要 Spring Security 身份认证，返回字符串 "HelloWorld"</li>
 *   <li>POST /login：用户名密码登录，返回 JWT 令牌</li>
 * </ul>
 */
@SpringBootApplication
public class SmallApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmallApplication.class, args);
    }
}
