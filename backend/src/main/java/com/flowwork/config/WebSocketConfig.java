package com.flowwork.config;

import com.flowwork.common.security.JwtUtil;
import com.flowwork.modules.auth.service.PermissionService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket (STOMP) 配置
 * 前端连接: /ws  订阅: /topic/project.{projectId}（项目广播）、/user/queue/notifications（个人通知）
 * 握手通过 query 参数 token 鉴权；已登录连接由 HandshakeHandler 映射 Principal（个人推送依赖它）
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;

    /**
     * 注册 STOMP 端点 /ws：挂载 JWT 握手拦截器与用户映射，
     * 放行本地前端来源，并启用 SockJS 降级支持
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new JwtHandshakeInterceptor())
                .setHandshakeHandler(new UserIdHandshakeHandler())
                .setAllowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173")
                .withSockJS();
    }

    /**
     * 消息代理配置：/topic、/queue 前缀的消息由内置简单代理广播；
     * 客户端发送消息统一以 /app 为前缀，由 @MessageMapping 处理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 握手期把 userId 映射为 Principal，使 convertAndSendToUser（/user/queue/*）可用
     */
    private static class UserIdHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                          Map<String, Object> attributes) {
            Object uid = attributes.get("userId");
            if (uid != null) {
                return () -> String.valueOf(uid);
            }
            return super.determineUser(request, wsHandler, attributes);
        }
    }

    /**
     * 握手鉴权：从 ?token= 解析 JWT，构造 Principal（userId）
     */
    private class JwtHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletReq) {
                String token = servletReq.getServletRequest().getParameter("token");
                if (token != null && !token.isBlank()) {
                    try {
                        Claims claims = jwtUtil.parse(token);
                        Long userId = Long.valueOf(claims.getSubject());
                        permissionService.loadUserOrThrow(userId);
                        attributes.put("userId", userId);
                        return true;
                    } catch (Exception e) {
                        log.warn("WebSocket 握手鉴权失败: {}", e.getMessage());
                    }
                }
            }
            return true; // 允许匿名握手，订阅项目频道的合法性由业务层校验
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
