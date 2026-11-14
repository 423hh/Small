package com.example.small.config;

import com.example.small.repository.UserRepository;
import com.example.small.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Security 配置。
 *
 * <p>规则：
 * <ul>
 *   <li>POST /login：公开访问，用于换取令牌</li>
 *   <li>GET /helloWorld：必须通过身份认证</li>
 *   <li>其他请求：同样必须通过身份认证（不再使用 Spring Security 默认的表单登录和 Basic 认证）</li>
 * </ul>
 *
 * <p>认证方式：无状态 JWT。登录成功后拿到令牌，之后每次请求带上
 * {@code Authorization: Bearer <token>}。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** 密码编码器：BCrypt。数据库中保存的是密文 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 从数据库读取用户，供 Spring Security 认证使用 */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(sysUser -> User.withUsername(sysUser.getUsername())
                        .password(sysUser.getPassword())
                        .roles("USER")
                        .disabled(!Boolean.TRUE.equals(sysUser.getEnabled()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
    }

    /** 认证管理器：登录接口用它来校验用户名和密码 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                // 前后端分离的无状态接口，关闭 CSRF 与默认登录页
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 不创建 HttpSession，身份信息只由令牌承载
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 可视化页面及其静态资源：公开访问，页面本身再去调用登录接口
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/favicon.svg").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/assets/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/helloWorld").authenticated()
                        .anyRequest().authenticated())
                // 未认证 / 无权限时返回 JSON，而不是跳转登录页
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        "未认证：请先调用 POST /login 获取令牌，并在请求头携带 Authorization: Bearer <token>"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                        "无权访问该资源")))
                // 把 JWT 过滤器放在用户名密码认证过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 统一输出 JSON 格式的错误响应 */
    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper,
                                  int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status);
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
