package com.avionics_systems.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Component
public class CorsWebFilter implements WebFilter, Ordered {

    @Value("${app.gateway.cors.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * No-op filter: CORS headers are handled by the YAML gateway config.
     * This filter was adding duplicate CORS headers alongside the YAML config.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}