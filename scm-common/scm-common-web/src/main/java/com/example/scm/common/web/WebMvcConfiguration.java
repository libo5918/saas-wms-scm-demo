package com.example.scm.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final Environment environment;
    private final boolean enforceGateway;
    private final String internalSecret;

    public WebMvcConfiguration(
            Environment environment,
            @Value("${security.gateway.enforce:false}") boolean enforceGateway,
            @Value("${security.gateway.internal-secret:}") String internalSecret
    ) {
        this.environment = environment;
        this.enforceGateway = enforceGateway;
        this.internalSecret = internalSecret;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantHeaderInterceptor(environment, enforceGateway, internalSecret))
                .addPathPatterns("/**");
    }
}
