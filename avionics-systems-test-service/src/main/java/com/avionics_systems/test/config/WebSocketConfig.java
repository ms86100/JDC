package com.avionics_systems.test.config;

import com.avionics_systems.cluster.event.ClusterEventBus;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ClusterEventBus clusterEventBus;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketConfig(ClusterEventBus clusterEventBus, @Lazy SimpMessagingTemplate messagingTemplate) {
        this.clusterEventBus = clusterEventBus;
        this.messagingTemplate = messagingTemplate;
    }

    private static final String CLUSTER_CHANNEL = "test-stomp";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/test-events")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @PostConstruct
    public void initClusterRelay() {
        clusterEventBus.subscribe(CLUSTER_CHANNEL, message -> {
            messagingTemplate.convertAndSend("/topic/test-events", message);
            log.debug("Relayed remote STOMP message to local subscribers");
        });
    }
}
