package com.rembyte.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Живые уведомления чата вместо постоянного HTTP-поллинга (раньше — каждые
 * 4-5 секунд с виджета и панели оператора, независимо от того, есть ли
 * что-то новое). Сам обработчик не носит данные — только сигнал «что-то
 * изменилось», клиент по нему просто дёргает уже существующий REST-запрос
 * обновления. Это сильно снижает число запросов/нагрузку на БД, но не
 * меняет формат/авторизацию самих данных.
 * <p>
 * Два вида подписчиков:
 * <ul>
 *   <li>панель оператора ({@code /ws/chat/operator}, только авторизованным
 *       ADMIN/OPERATOR — см. SecurityConfig) — получает любое событие чата;</li>
 *   <li>виджет на сайте клиента ({@code /ws/chat/public/{token}}, анонимно,
 *       как и остальной публичный чат-API) — получает только события своего
 *       диалога, определяемого по {@code publicToken} в пути подключения.</li>
 * </ul>
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    public static final String PUBLIC_TOKEN_ATTRIBUTE = "publicToken";

    private final Set<WebSocketSession> operatorSessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<WebSocketSession>> widgetSessionsByToken = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = publicToken(session);
        if (token != null) {
            widgetSessionsByToken.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet()).add(session);
        } else {
            operatorSessions.add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        operatorSessions.remove(session);
        String token = publicToken(session);
        if (token != null) {
            widgetSessionsByToken.computeIfPresent(token, (k, sessions) -> {
                sessions.remove(session);
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Клиент ничего не отправляет (соединение только для push от сервера) —
        // любой входящий текст (например, keep-alive "ping") просто игнорируем.
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignored) {
            // сессия уже мертва — не страшно
        }
    }

    /** Разослать событие всем открытым сессиям панели оператора. */
    public void notifyOperators(String jsonPayload) {
        broadcast(operatorSessions, jsonPayload);
    }

    /** Разослать событие сессиям виджета конкретного диалога. */
    public void notifyWidget(String publicToken, String jsonPayload) {
        if (publicToken == null) {
            return;
        }
        Set<WebSocketSession> sessions = widgetSessionsByToken.get(publicToken);
        if (sessions != null) {
            broadcast(sessions, jsonPayload);
        }
    }

    private void broadcast(Set<WebSocketSession> sessions, String jsonPayload) {
        if (sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(jsonPayload);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.debug("Не удалось отправить событие чата в сессию {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    private String publicToken(WebSocketSession session) {
        Object value = session.getAttributes().get(PUBLIC_TOKEN_ATTRIBUTE);
        return value instanceof String s && !s.isBlank() ? s : null;
    }
}
