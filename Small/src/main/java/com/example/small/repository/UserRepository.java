package com.example.small.repository;

import com.example.small.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问接口。
 */
public interface UserRepository extends JpaRepository<SysUser, Long> {

    /** 按用户名查询用户 */
    Optional<SysUser> findByUsername(String username);
}
