package com.example.scm.aiagent.config;

import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Tool 调用审计 MySQL 数据源配置。
 *
 * <p>仅在 {@code ai.agent.tools.audit.mode=mysql} 时启用，默认 in-memory 测试路径不会依赖 MySQL。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "ai.agent.tools.audit", name = "mode", havingValue = "mysql")
@MapperScan(basePackages = "com.example.scm.aiagent.tool.persistence.mapper",
        sqlSessionFactoryRef = "toolAuditSqlSessionFactory")
public class ToolAuditMysqlDataSourceConfiguration {

    /**
     * 创建或复用 AI Agent 的 MySQL DataSource。
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource toolAuditDataSource(AiAgentProperties properties) {
        AiAgentProperties.MysqlRegistryProperties mysql = properties.getRag().getRegistry().getMysql();
        if (mysql == null || !StringUtils.hasText(mysql.getUrl())) {
            throw new IllegalStateException("ai.agent.rag.registry.mysql.url must be configured when tools.audit.mode=mysql");
        }
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(mysql.getUrl());
        dataSource.setUser(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        log.info("Tool audit MySQL DataSource created, urlConfigured={}, usernameConfigured={}",
                StringUtils.hasText(mysql.getUrl()), StringUtils.hasText(mysql.getUsername()));
        return dataSource;
    }

    /**
     * 创建 Tool audit 专用 MyBatis SqlSessionFactory。
     */
    @Bean
    public SqlSessionFactory toolAuditSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/tool/*.xml"));
        return factoryBean.getObject();
    }

    /**
     * 创建 Tool audit 专用 SqlSessionTemplate。
     */
    @Bean
    public SqlSessionTemplate toolAuditSqlSessionTemplate(
            @Qualifier("toolAuditSqlSessionFactory") SqlSessionFactory toolAuditSqlSessionFactory) {
        return new SqlSessionTemplate(toolAuditSqlSessionFactory);
    }

    /**
     * 创建 Tool audit 专用事务管理器。
     */
    @Bean
    public DataSourceTransactionManager toolAuditTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
