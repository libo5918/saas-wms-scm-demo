package com.example.scm.aiagent.config;

import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * RAG Document Registry 的 MySQL 数据源配置。
 *
 * <p>该配置只在 {@code ai.agent.rag.registry.mode=mysql} 时启用。默认 in-memory 模式不会创建
 * DataSource，也不会要求本地必须安装 MySQL。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "ai.agent.rag.registry", name = "mode", havingValue = "mysql")
@MapperScan(basePackages = "com.example.scm.aiagent.rag.persistence.mapper",
        sqlSessionFactoryRef = "ragRegistrySqlSessionFactory")
public class RagRegistryMysqlDataSourceConfiguration {

    /**
     * 创建 RAG Registry 专用 DataSource。
     *
     * <p>如果应用容器中已经存在 DataSource，会优先复用已有数据源；否则根据
     * {@code ai.agent.rag.registry.mysql.*} 创建一个轻量 MySQL DataSource。</p>
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource ragRegistryDataSource(AiAgentProperties properties) {
        AiAgentProperties.MysqlRegistryProperties mysql = properties.getRag().getRegistry().getMysql();
        if (mysql == null || !StringUtils.hasText(mysql.getUrl())) {
            throw new IllegalStateException("ai.agent.rag.registry.mysql.url must be configured when registry.mode=mysql");
        }
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(mysql.getUrl());
        dataSource.setUser(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        log.info("RAG MySQL registry DataSource created, urlConfigured={}, usernameConfigured={}",
                StringUtils.hasText(mysql.getUrl()), StringUtils.hasText(mysql.getUsername()));
        return dataSource;
    }

    /**
     * 创建 RAG Registry 专用 MyBatis SqlSessionFactory。
     */
    @Bean
    public SqlSessionFactory ragRegistrySqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/rag/*.xml"));
        return factoryBean.getObject();
    }

    /**
     * 创建 RAG Registry 专用 SqlSessionTemplate。
     */
    @Bean
    public SqlSessionTemplate ragRegistrySqlSessionTemplate(SqlSessionFactory ragRegistrySqlSessionFactory) {
        return new SqlSessionTemplate(ragRegistrySqlSessionFactory);
    }

    /**
     * 创建 RAG Registry 专用事务管理器，用于保存导入批次和批次文档关联。
     */
    @Bean
    public DataSourceTransactionManager ragRegistryTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
