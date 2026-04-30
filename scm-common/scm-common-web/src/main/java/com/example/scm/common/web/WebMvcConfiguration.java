package com.example.scm.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final TenantHeaderInterceptor tenantHeaderInterceptor;

    public WebMvcConfiguration(TenantHeaderInterceptor tenantHeaderInterceptor) {
        this.tenantHeaderInterceptor = tenantHeaderInterceptor;
    }

    @Bean
    public TenantHeaderInterceptor tenantHeaderInterceptor(
            Environment environment,
            @Value("${security.gateway.enforce:false}") boolean enforceGateway,
            @Value("${security.gateway.internal-secret:}") String internalSecret
    ) {
        return new TenantHeaderInterceptor(environment, enforceGateway, internalSecret);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantHeaderInterceptor).addPathPatterns("/**");
    }
}
