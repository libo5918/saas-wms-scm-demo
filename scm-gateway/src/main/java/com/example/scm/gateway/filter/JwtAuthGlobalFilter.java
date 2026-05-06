package com.example.scm.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.security.GatewayHeaders;
import com.example.scm.common.security.JwtTokenClaims;
import com.example.scm.common.security.JwtTokenProvider;
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

import java.util.List;
import java.util.Map;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final String internalSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> whitelist = List.of(
            "/api/v1/auth/login",
            "/actuator/**"
    );

    public JwtAuthGlobalFilter(
            @Value("${auth.jwt.issuer:scm-auth}") String issuer,
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.expire-seconds:7200}") long expireSeconds,
            @Value("${security.gateway.internal-secret:}") String internalSecret
    ) {
        this.jwtTokenProvider = new JwtTokenProvider(issuer, secret, expireSeconds);
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
        JwtTokenClaims claims;
        try {
            claims = jwtTokenProvider.parseToken(authHeader.substring(7));
        } catch (BusinessException ex) {
            return unauthorized(exchange.getResponse(), ex.getMessage());
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(GatewayHeaders.TENANT_ID, String.valueOf(claims.tenantId()))
                .header(GatewayHeaders.USER_ID, String.valueOf(claims.userId()))
                .header(GatewayHeaders.USERNAME, claims.username() == null ? "" : claims.username())
                .header(GatewayHeaders.USER_ROLES, String.join(",", claims.roles() == null ? List.of() : claims.roles()))
                .header(GatewayHeaders.GATEWAY_INTERNAL, "true")
                .header(GatewayHeaders.GATEWAY_SECRET, internalSecret)
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
            body = "{\"success\":false,\"code\":\"401\",\"message\":\"Unauthorized\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
