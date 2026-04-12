package com.example.scm.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final TenantHeaderInterceptor tenantHeaderInterceptor;

    public WebMvcConfiguration(TenantHeaderInterceptor tenantHeaderInterceptor) {
        this.tenantHeaderInterceptor = tenantHeaderInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantHeaderInterceptor).addPathPatterns("/**");
    }
}
