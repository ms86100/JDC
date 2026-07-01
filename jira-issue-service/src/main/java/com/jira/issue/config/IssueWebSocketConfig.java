package com.jira.issue.config;

import com.jira.issue.event.IssueRealtimeBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class IssueWebSocketConfig implements WebSocketConfigurer {

    private final IssueRealtimeBroadcaster broadcaster;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                broadcaster.register(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                broadcaster.unregister(session);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                /* ping/pong optional */
            }
        }, "/ws/issues").setAllowedOrigins("*");
    }
}
