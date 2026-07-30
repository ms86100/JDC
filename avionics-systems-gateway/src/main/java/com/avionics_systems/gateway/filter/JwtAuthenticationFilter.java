package com.avionics_systems.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final SecretKey secretKey;
    private final List<String> publicPaths;
    private final String userIdHeader;
    private final String usernameHeader;
    private final String userRolesHeader;

    public JwtAuthenticationFilter(
            @Value("${jwt.secret}") String secret,
            @Value("${app.gateway.public-paths:/api/auth/login,/api/auth/register,/api/auth/refresh,/v3/api-docs/**,/swagger-ui/**,/swagger-resources/**,/actuator/health,/ws/**}") String publicPathsStr,
            @Value("${app.gateway.headers.user-id:X-User-Id}") String userIdHeader,
            @Value("${app.gateway.headers.username:X-Username}") String usernameHeader,
            @Value("${app.gateway.headers.user-roles:X-User-Roles}") String userRolesHeader) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.publicPaths = Arrays.asList(publicPathsStr.split(","));
        this.userIdHeader = userIdHeader;
        this.usernameHeader = usernameHeader;
        this.userRolesHeader = userRolesHeader;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // Allow OPTIONS preflight requests without auth
        if ("OPTIONS".equals(method)) {
            return chain.filter(exchange);
        }

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(userIdHeader, userId)
                    .header(usernameHeader, username != null ? username : "")
                    .header(userRolesHeader, roles != null ? String.join(",", roles) : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return path.startsWith(prefix);
            }
            return path.equals(pattern) || path.startsWith(pattern + "/");
        });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
