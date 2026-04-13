package com.example.scm.purchase.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.scm.purchase.mapper")
public class PurchaseMybatisScanConfiguration {
}
