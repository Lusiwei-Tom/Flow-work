package com.flowwork.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类：基于 HS256 对称签名，实现双 Token 机制
 * - Access Token（访问令牌）：短时效（默认 2 小时），携带用户角色与权限码
 * - Refresh Token（刷新令牌）：长时效（默认 7 天），仅携带 userId + deviceId，用于换取新 Access Token
 * 配置项：flowwork.jwt.secret / flowwork.jwt.access-token-ttl / flowwork.jwt.refresh-token-ttl
 */
@Slf4j
@Component
public class JwtUtil {

    /** HMAC 签名密钥，从配置文件读取，用于签发和验签 */
    @Value("${flowwork.jwt.secret}")
    private String secret;
    /** Access Token 过期时间（秒），默认 7200 = 2 小时 */
    @Value("${flowwork.jwt.access-token-ttl:7200}")
    private long accessTtl;
    /** Refresh Token 过期时间（秒），默认 604800 = 7 天 */
    @Value("${flowwork.jwt.refresh-token-ttl:604800}")
    private long refreshTtl;

    /** HMAC-SHA 密钥对象，init() 中由 secret 字符串构造 */
    private SecretKey key;

    /**
     * Bean 初始化后执行：将 secret 字符串转为 HMAC-SHA 密钥
     * @PostConstruct 保证在依赖注入完成后、业务调用前执行
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 Access Token（访问令牌）
     * payload 中携带 userId（subject）、username、roles、perms，供 JwtAuthenticationFilter 解析鉴权
     * 过期时间由 accessTtl 控制（默认 2 小时）
     */
    public String createAccessToken(Long userId, String username, List<String> roles, List<String> perms) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // JWT 唯一 ID（jti），用于唯一标识该 token
                .subject(String.valueOf(userId)) // 主体：userId（sub）
                .claim("username", username) // 自定义声明：用户名
                .claim("roles", roles) // 自定义声明：角色列表
                .claim("perms", perms) // 自定义声明：权限码列表
                .issuedAt(now) // 签发时间（iat）
                .expiration(new Date(now.getTime() + accessTtl * 1000)) // 过期时间（exp）
                .signWith(key) // 用 HMAC-SHA 密钥签名
                .compact(); // 构建最终 JWT 字符串
    }

    /**
     * 创建 Refresh Token（刷新令牌）
     * 仅携带 userId + deviceId，不携带角色权限（最小化 payload）
     * 过期时间由 refreshTtl 控制（默认 7 天），用于在 Access Token 过期后换取新 token
     */
    public String createRefreshToken(Long userId, String deviceId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // JWT 唯一 ID
                .subject(String.valueOf(userId)) // 主体：userId
                .claim("deviceId", deviceId) // 自定义声明：设备标识（用于设备级 token 管理）
                .issuedAt(now) // 签发时间
                .expiration(new Date(now.getTime() + refreshTtl * 1000)) // 过期时间
                .signWith(key) // 签名
                .compact(); // 构建 JWT 字符串
    }

    /**
     * 解析并验签 JWT：签名错误/过期/格式异常时抛出异常
     * 返回 Claims（payload 内容），调用方通过 claims.getSubject() 获取 userId 等
     */
    public Claims parse(String token) {
        return Jwts.parser() // 创建 JWT 解析器
                .verifyWith(key) // 设置验签密钥（与签发时同一密钥）
                .build() // 构建解析器
                .parseSignedClaims(token) // 解析并验签，签名无效或过期会抛异常
                .getPayload(); // 获取 payload（Claims）
    }

    /**
     * 从 Claims 中安全读取 String 列表（如 roles、perms）
     * 字段不存在或类型不匹配时返回空列表，避免 NPE
     */
    @SuppressWarnings("unchecked") // 忽略未检查的类型转换警告（Claims 动态类型无法保证 List<String>）
    public List<String> getStringList(Claims claims, String name) {
        Object v = claims.get(name);
        return v instanceof List ? (List<String>) v : List.of();
    }

    /**
     * Claims 转为 Map（Claims 本身即 Map<String,Object> 的子接口，直接返回）
     */
    public Map<String, Object> claimsToMap(Claims claims) {
        return claims;
    }

    /** 获取 Refresh Token 过期时间（秒），供 AuthService 计算刷新 token 的新过期时间 */
    public long getRefreshTtl() {
        return refreshTtl;
    }

    /** 获取 Access Token 过期时间（秒），供 Controller 返回给前端设置过期提醒 */
    public long getAccessTtl() {
        return accessTtl;
    }
}
