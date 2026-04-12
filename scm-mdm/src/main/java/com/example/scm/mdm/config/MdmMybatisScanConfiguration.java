package com.example.scm.mdm.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.scm.mdm.mapper")
public class MdmMybatisScanConfiguration {
}
