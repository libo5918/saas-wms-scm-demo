package com.example.scm.gateway.filter;

import com.example.scm.common.security.GatewayHeaders;
import com.example.scm.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtAuthGlobalFilterTest {

    private static final String SECRET = "change-this-dev-secret-key-at-least-32-bytes";

    @Test
    void shouldInjectGatewayHeadersForValidToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider("scm-auth", SECRET, 7200);
        String token = tokenProvider.generateToken(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        JwtAuthGlobalFilter filter = new JwtAuthGlobalFilter("scm-auth", SECRET, 7200, "internal-secret");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/inventory/balances")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );
        AtomicReference<ServerHttpRequest> mutatedRequest = new AtomicReference<>();
        GatewayFilterChain chain = serverWebExchange -> {
            mutatedRequest.set(serverWebExchange.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerHttpRequest request = mutatedRequest.get();
        assertNotNull(request);
        assertEquals("1", request.getHeaders().getFirst(GatewayHeaders.TENANT_ID));
        assertEquals("10001", request.getHeaders().getFirst(GatewayHeaders.USER_ID));
        assertEquals("admin", request.getHeaders().getFirst(GatewayHeaders.USERNAME));
        assertEquals("ROLE_ADMIN", request.getHeaders().getFirst(GatewayHeaders.USER_ROLES));
        assertEquals("true", request.getHeaders().getFirst(GatewayHeaders.GATEWAY_INTERNAL));
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {
        JwtAuthGlobalFilter filter = new JwtAuthGlobalFilter("scm-auth", SECRET, 7200, "internal-secret");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/inventory/balances").build()
        );

        filter.filter(exchange, serverWebExchange -> Mono.empty()).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }
}
