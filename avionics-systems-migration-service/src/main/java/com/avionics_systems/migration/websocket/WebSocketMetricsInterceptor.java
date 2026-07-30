package com.avionics_systems.migration.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class WebSocketMetricsInterceptor implements ChannelInterceptor {

    private final Counter messagesSent;
    private final Counter messagesReceived;
    private final Counter connectionCounter;
    private final Counter disconnectionCounter;
    private final Counter errorCounter;
    private final Timer messageProcessingTime;
    private final ConcurrentHashMap<String, Long> messageCounts = new ConcurrentHashMap<>();

    public WebSocketMetricsInterceptor(MeterRegistry registry) {
        this.messagesSent = Counter.builder("websocket.messages.sent")
                .description("Total WebSocket messages sent")
                .register(registry);

        this.messagesReceived = Counter.builder("websocket.messages.received")
                .description("Total WebSocket messages received")
                .register(registry);

        this.connectionCounter = Counter.builder("websocket.connections.established")
                .description("Total WebSocket connections established")
                .register(registry);

        this.disconnectionCounter = Counter.builder("websocket.connections.closed")
                .description("Total WebSocket connections closed")
                .register(registry);

        this.errorCounter = Counter.builder("websocket.errors")
                .description("Total WebSocket errors")
                .register(registry);

        this.messageProcessingTime = Timer.builder("websocket.message.processing.time")
                .description("Time taken to process WebSocket messages")
                .register(registry);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        long startTime = System.nanoTime();

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            if (command != null) {
                messageCounts.merge(command.name(), 1L, Long::sum);

                switch (command) {
                    case CONNECT -> connectionCounter.increment();
                    case DISCONNECT -> disconnectionCounter.increment();
                    case ERROR -> errorCounter.increment();
                    default -> messagesReceived.increment();
                }
            }
        }

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel,
                                    boolean sent, Exception ex) {
        if (ex != null) {
            errorCounter.increment();
            log.error("WebSocket message send error", ex);
        }

        long duration = System.nanoTime() - messageProcessingTime.count();
        messageProcessingTime.record(Duration.ofNanos(duration));
    }

    public ConcurrentHashMap<String, Long> getMessageCounts() {
        return new ConcurrentHashMap<>(messageCounts);
    }
}