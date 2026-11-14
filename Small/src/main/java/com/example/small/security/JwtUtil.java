package com.example.small.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简易 JWT 工具：使用 JDK 自带的 HMAC-SHA256 生成和校验令牌，不依赖第三方库。
 *
 * <p>令牌结构与标准 JWT 一致：header.payload.signature（Base64URL 编码）。
 */
@Component
public class JwtUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secretBytes;
    private final long expireMillis;

    public JwtUtil(ObjectMapper objectMapper,
                   @Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-minutes:120}") long expireMinutes) {
        this.objectMapper = objectMapper;
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expireMillis = expireMinutes * 60L * 1000L;
    }

    /** 根据用户名生成令牌 */
    public String generateToken(String username) {
        long now = System.currentTimeMillis();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("iat", now / 1000);
        payload.put("exp", (now + expireMillis) / 1000);

        try {
            String headerPart = ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadPart = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String content = headerPart + "." + payloadPart;
            return content + "." + sign(content);
        } catch (Exception e) {
            throw new IllegalStateException("生成令牌失败", e);
        }
    }

    /**
     * 校验令牌的签名与有效期，返回令牌中的用户名。
     *
     * @throws IllegalArgumentException 令牌格式错误、签名不匹配或已过期
     */
    public String parseUsername(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("令牌格式不正确");
        }

        String content = parts[0] + "." + parts[1];
        if (!sign(content).equals(parts[2])) {
            throw new IllegalArgumentException("令牌签名不合法");
        }

        try {
            Map<?, ?> payload = objectMapper.readValue(DECODER.decode(parts[1]), Map.class);
            Object expValue = payload.get("exp");
            if (expValue instanceof Number exp && System.currentTimeMillis() / 1000 >= exp.longValue()) {
                throw new IllegalArgumentException("令牌已过期");
            }
            Object subject = payload.get("sub");
            if (subject == null) {
                throw new IllegalArgumentException("令牌缺少用户信息");
            }
            return subject.toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("令牌解析失败", e);
        }
    }

    /** 令牌有效期（秒） */
    public long getExpireSeconds() {
        return expireMillis / 1000;
    }

    /** 对 header.payload 做 HMAC-SHA256 签名 */
    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签名失败", e);
        }
    }
}
