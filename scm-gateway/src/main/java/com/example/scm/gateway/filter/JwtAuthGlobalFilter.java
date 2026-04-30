package com.example.scm.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-User-Name";
    private static final String HEADER_GATEWAY_INTERNAL = "X-Gateway-Internal";
    private static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";

    private final SecretKey secretKey;
    private final String internalSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> whitelist = List.of(
            "/api/v1/auth/login",
            "/actuator/**"
    );

    public JwtAuthGlobalFilter(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${security.gateway.internal-secret:}") String internalSecret
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.internalSecret = internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            return unauthorized(exchange.getResponse(), "Invalid token");
        }

        Object tenantId = claims.get("tenantId");
        Object userId = claims.get("userId");
        Object username = claims.get("username");
        if (tenantId == null || userId == null) {
            return unauthorized(exchange.getResponse(), "Token missing tenantId or userId");
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER_TENANT_ID, String.valueOf(tenantId))
                .header(HEADER_USER_ID, String.valueOf(userId))
                .header(HEADER_USERNAME, username == null ? "" : String.valueOf(username))
                .header(HEADER_GATEWAY_INTERNAL, "true")
                .header(HEADER_GATEWAY_SECRET, internalSecret)
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : whitelist) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Map.of(
                    "success", false,
                    "code", "401",
                    "message", message
            ));
        } catch (JsonProcessingException ex) {
            body = "{\"success\":false,\"code\":\"401\",\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
