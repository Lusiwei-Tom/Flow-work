package com.flowwork.common.security;

import com.flowwork.modules.auth.service.PermissionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 *：拦截所有 HTTP 请求，尝试从请求头中解析 JWT Token。如果 Token 合法，就把用户信息“塞进”Spring Security 的
 上下文（SecurityContext）中，完成认证；如果 Token 非法，就清空上下文，放行请求，交给后续的 Spring Security 机制去拦截。
 */
@Slf4j
@Component
@RequiredArgsConstructor
//继承 OncePerRequestFilter，每个请求只执行一次，避免重复校验（如在 SecurityFilter）
// 该过滤器负责从请求头解析 Bearer Token，校验签名/过期，构造 Authentication 放入 SecurityContext
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类：解析/校验 token */
    private final JwtUtil jwtUtil;
    /** 权限服务：实时校验用户状态（禁用立即失效）并加载权限 */
    private final PermissionService permissionService;
    /** Ant 路径匹配器：支持通配符匹配（如 /api/**） */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 核心过滤逻辑：解析 JWT → 校验用户 → 填充 SecurityContext → 放行请求
     * 无论 token 是否有效，最终都会调用 chain.doFilter 放行，
     * 受保护接口的拒绝由 SecurityConfig 的 authenticationEntryPoint（401）完成
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 1. 从请求头提取 Bearer Token（格式：Authorization: Bearer xxx.yyy.zzz）
        String header = request.getHeader("Authorization");// 从请求头获取 Authorization 头
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // 去掉 "Bearer " 前缀，取纯 token
            try {
                // 2. 解析 JWT，提取 userId（subject）
                Claims claims = jwtUtil.parse(token);//
                Long userId = Long.valueOf(claims.getSubject());
                // 3. 实时校验用户状态：禁用用户即使 token 未过期也立即失效
                permissionService.loadUserOrThrow(userId);// 加载用户信息，若用户禁用则抛出异常

                // 4. 从 JWT 中读取权限码列表，加 PERM_ 前缀转为 Spring Security authority
                List<String> perms = jwtUtil.getStringList(claims, "perms");
                List<SimpleGrantedAuthority> authorities = perms.stream()
                        .map(p -> new SimpleGrantedAuthority("PERM_" + p))
                        .toList();
                // 5. 构造认证对象（principal=userId），放入 SecurityContext 供后续切面/鉴权使用
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // token 无效/过期/用户禁用：清除上下文，不放行受保护接口
                SecurityContextHolder.clearContext();
            }
        }
        // 无论有无 token，继续过滤器链：受保护接口由 SecurityConfig 的 EntryPoint 返回 401
        chain.doFilter(request, response);
    }
}
