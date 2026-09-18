package com.rembyte.config;

import com.rembyte.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Регистрирует «сырые» WebSocket-эндпоинты чата (без STOMP — он тут не
 * нужен: сообщения только сигнальные, см. {@link ChatWebSocketHandler}).
 * Авторизация — на уровне HTTP-запроса апгрейда, в {@code SecurityConfig}:
 * {@code /ws/chat/operator} требует сессию ADMIN/OPERATOR как и остальная
 * панель, {@code /ws/chat/public/**} открыт как и остальной публичный
 * чат-виджет (гостям он и нужен).
 */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public ChatWebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat/operator")
                .setAllowedOriginPatterns("*");

        registry.addHandler(chatWebSocketHandler, "/ws/chat/public/**")
                .addInterceptors(new PublicTokenHandshakeInterceptor())
                // Виджет встраивается iframe'ом на сторонних сайтах — соединение
                // всё равно идёт напрямую на наш домен, но со страницы чужого origin.
                .setAllowedOriginPatterns("*");
    }

    /** Достаёт последний сегмент пути ({@code .../public/{token}}) и кладёт его в атрибуты сессии. */
    private static class PublicTokenHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String path = request.getURI().getPath();
            String token = path.substring(path.lastIndexOf('/') + 1).trim();
            if (token.isEmpty()) {
                return false;
            }
            attributes.put(ChatWebSocketHandler.PUBLIC_TOKEN_ATTRIBUTE, token);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Exception exception) {
            // ничего
        }
    }
}
