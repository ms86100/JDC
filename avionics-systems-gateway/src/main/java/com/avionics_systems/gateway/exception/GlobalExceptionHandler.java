package com.avionics_systems.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Map;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = messageSource.getMessage("gateway.error.unexpected", null, "An unexpected error occurred", Locale.ENGLISH);

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : message;
        } else if (ex.getCause() instanceof ResponseStatusException cause) {
            status = HttpStatus.valueOf(cause.getStatusCode().value());
            message = cause.getReason() != null ? cause.getReason() : message;
        }

        log.error("Gateway error: {} - {} - {}", status.value(), ex.getClass().getSimpleName(), ex.getMessage());

        response.setStatusCode(status);

        String json = String.format(
            "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
            java.time.LocalDateTime.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            message.replace("\"", "\\\"")
        );

        byte[] bytes = json.getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}