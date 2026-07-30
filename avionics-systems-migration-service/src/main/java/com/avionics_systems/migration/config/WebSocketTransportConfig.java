package com.avionics_systems.migration.config;

import com.avionics_systems.migration.websocket.WebSocketMetricsInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketTransportConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketMetricsInterceptor metricsInterceptor;

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024)
                .setSendBufferSizeLimit(512 * 1024);

        log.info("WebSocket transport limits configured");
    }
}
