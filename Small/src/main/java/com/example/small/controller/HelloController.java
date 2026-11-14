package com.example.small.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HelloWorld 接口。
 *
 * <p>该接口在 SecurityConfig 中被配置为需要认证：
 * 未携带有效令牌返回 401，携带有效令牌返回字符串 "HelloWorld"。
 */
@RestController
public class HelloController {

    /**
     * GET /helloWorld
     *
     * @return 字符串 HelloWorld
     */
    @GetMapping("/helloWorld")
    public String helloWorld() {
        return "HelloWorld";
    }
}
