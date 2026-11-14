package com.example.small.init;

import com.example.small.entity.SysUser;
import com.example.small.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时初始化测试账号：test / 123456。
 *
 * <p>密码使用 BCrypt 加密后入库，表中不保存明文。已存在同名用户时不再重复插入。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_USERNAME = "test";
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(DEFAULT_USERNAME).isPresent()) {
            log.info("测试用户已存在，跳过初始化：{}", DEFAULT_USERNAME);
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(DEFAULT_USERNAME);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setNickname("测试用户");
        user.setEnabled(Boolean.TRUE);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("已初始化测试用户：{} / {}", DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
}
