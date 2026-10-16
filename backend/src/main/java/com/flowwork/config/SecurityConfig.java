package com.flowwork.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowwork.common.result.Result;
import com.flowwork.common.result.ResultCode;
import com.flowwork.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import jakarta.servlet.DispatcherType;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 安全配置（前后端分离 + JWT 无状态认证）
 * - 关闭 CSRF 与 Session（STATELESS），认证由 JwtAuthenticationFilter 完成
 * - 放行白名单接口（登录/注册/验证码/WebSocket/接口文档等）
 * - 统一返回 401/403 的 JSON 错误响应
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器：解析 token 并向 SecurityContext 填充认证信息 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 无需登录即可访问的接口白名单 */
    private static final String[] WHITELIST = {
            "/api/auth/login",
            "/api/auth/login/phone",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/registration-applications",
            "/api/auth/sms/code",
            "/ws/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            "/favicon.ico"
    };

    /**
     * 密码编码器：BCrypt 加密（注册加密与登录校验共用）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链主配置：无状态会话 + 白名单放行 + JWT 过滤器 + 统一异常响应
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE/Flux 走 Servlet AsyncContext 异步分发，SecurityContext(ThreadLocal)
                        // 在异步线程丢失会导致二次鉴权 Access Denied；权限已由 Controller 入口校验
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, resp, e) -> writeError(resp, ResultCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, resp, e) -> writeError(resp, ResultCode.FORBIDDEN)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 向客户端写入统一格式的 401/403 JSON 错误响应
     */
    private void writeError(jakarta.servlet.http.HttpServletResponse resp, ResultCode rc) throws java.io.IOException {
        resp.setStatus(rc == ResultCode.FORBIDDEN ? 403 : 401);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(new ObjectMapper().writeValueAsString(Result.fail(rc)));
    }

    /**
     * 跨域配置：仅放行本地前端开发端口（5173），允许携带凭证，预检结果缓存 1 小时
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
