package com.example.small.dto;

/**
 * 登录成功后的响应体。
 */
public class LoginResponse {

    /** 用户名 */
    private String username;

    /** 访问令牌（JWT） */
    private String token;

    /** 令牌类型，固定为 Bearer */
    private String tokenType = "Bearer";

    /** 令牌有效期（秒） */
    private long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(String username, String token, long expiresIn) {
        this.username = username;
        this.token = token;
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
