package com.rembyte.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Раздача сигналов чата по WebSocket: сессии оператора и виджета по токену. */
class ChatWebSocketHandlerTest {

    @Test
    void operatorSessionReceivesEvent() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession operatorSession = mockSession(Map.of()); // без publicToken — считается оператором

        handler.afterConnectionEstablished(operatorSession);
        handler.notifyOperators("{\"type\":\"message\",\"conversationId\":1}");

        verify(operatorSession).sendMessage(new TextMessage("{\"type\":\"message\",\"conversationId\":1}"));
    }

    @Test
    void widgetSessionOnlyReceivesOwnConversationEvents() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession sessionA = mockSession(Map.of(ChatWebSocketHandler.PUBLIC_TOKEN_ATTRIBUTE, "token-A"));
        WebSocketSession sessionB = mockSession(Map.of(ChatWebSocketHandler.PUBLIC_TOKEN_ATTRIBUTE, "token-B"));

        handler.afterConnectionEstablished(sessionA);
        handler.afterConnectionEstablished(sessionB);

        handler.notifyWidget("token-A", "{\"type\":\"message\",\"conversationId\":7}");

        verify(sessionA).sendMessage(any());
        verify(sessionB, never()).sendMessage(any());
    }

    @Test
    void operatorsDoNotReceiveWidgetEvents() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession operatorSession = mockSession(Map.of());
        WebSocketSession widgetSession = mockSession(Map.of(ChatWebSocketHandler.PUBLIC_TOKEN_ATTRIBUTE, "token-A"));

        handler.afterConnectionEstablished(operatorSession);
        handler.afterConnectionEstablished(widgetSession);

        handler.notifyWidget("token-A", "payload");

        verify(operatorSession, never()).sendMessage(any());
        verify(widgetSession).sendMessage(any());
    }

    @Test
    void closedSessionStopsReceivingEvents() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession operatorSession = mockSession(Map.of());

        handler.afterConnectionEstablished(operatorSession);
        handler.afterConnectionClosed(operatorSession, CloseStatus.NORMAL);
        handler.notifyOperators("payload");

        verify(operatorSession, never()).sendMessage(any());
    }

    @Test
    void deadSessionIsSkippedWithoutBreakingOthers() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession broken = mockSession(Map.of());
        when(broken.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("boom")).when(broken).sendMessage(any());

        WebSocketSession healthy = mockSession(Map.of());

        handler.afterConnectionEstablished(broken);
        handler.afterConnectionEstablished(healthy);

        handler.notifyOperators("payload"); // не должно бросить исключение наружу

        verify(healthy).sendMessage(any());
    }

    private static WebSocketSession mockSession(Map<String, Object> attributes) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        java.util.Map<String, Object> mutable = new java.util.HashMap<>(attributes);
        when(session.getAttributes()).thenReturn(mutable);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("session-" + System.identityHashCode(session));
        return session;
    }
}
