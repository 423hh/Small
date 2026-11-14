package com.example.small.controller;

import com.example.small.dto.LoginRequest;
import com.example.small.dto.LoginResponse;
import com.example.small.security.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录接口。
 */
@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /login
     *
     * <p>请求体：{"username":"test","password":"123456"}
     * <p>成功返回 200 + 令牌；用户名或密码错误返回 401。
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 交给 Spring Security 的用户名密码认证，内部会调用 UserDetailsService 查库并比对密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            String username = authentication.getName();
            String token = jwtUtil.generateToken(username);

            log.info("用户登录成功：{}", username);
            return ResponseEntity.ok(new LoginResponse(username, token, jwtUtil.getExpireSeconds()));

        } catch (BadCredentialsException ex) {
            return error(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        } catch (UsernameNotFoundException ex) {
            return error(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        } catch (Exception ex) {
            log.warn("登录异常：{}", ex.getMessage());
            return error(HttpStatus.UNAUTHORIZED, "登录失败：" + ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
