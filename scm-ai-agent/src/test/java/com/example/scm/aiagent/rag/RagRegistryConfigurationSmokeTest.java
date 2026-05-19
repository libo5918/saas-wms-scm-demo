package com.example.scm.aiagent.rag;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.config.RagRegistryMysqlDataSourceConfiguration;
import com.example.scm.aiagent.rag.persistence.mapper.RagDocumentRegistryMapper;
import com.example.scm.aiagent.rag.service.InMemoryRagDocumentRegistry;
import com.example.scm.aiagent.rag.service.MysqlRagDocumentRegistry;
import com.example.scm.aiagent.rag.service.RagDocumentRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.model.image=none",
        "spring.ai.model.audio.speech=none",
        "spring.ai.model.audio.transcription=none",
        "spring.ai.model.moderation=none",
        "spring.ai.model.rerank=none",
        "spring.ai.model.video=none",
        "ai.agent.rag.registry.mode=in-memory"
})
class RagRegistryConfigurationSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RagDocumentRegistry ragDocumentRegistry;

    @Autowired
    private AiAgentProperties properties;

    @Test
    void shouldUseInMemoryRegistryByDefaultWithoutMysqlConnection() {
        assertInstanceOf(InMemoryRagDocumentRegistry.class, ragDocumentRegistry);
        assertEquals("in-memory", properties.getRag().getRegistry().getMode());
        assertEquals(0, applicationContext.getBeanNamesForType(MysqlRagDocumentRegistry.class, false, false).length);
    }

    @Test
    void shouldBindMysqlRegistryPropertiesAndCreateMysqlRegistryWithMapper() {
        new ApplicationContextRunner()
                .withUserConfiguration(MysqlRegistryTestConfiguration.class)
                .withBean(RagDocumentRegistryMapper.class, () -> mock(RagDocumentRegistryMapper.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues(
                        "ai.agent.rag.registry.mode=mysql",
                        "ai.agent.rag.registry.mysql.url=jdbc:mysql://127.0.0.1:3306/scm_ai_agent",
                        "ai.agent.rag.registry.mysql.username=root",
                        "ai.agent.rag.registry.mysql.password=secret"
                )
                .run(context -> {
                    AiAgentProperties contextProperties = context.getBean(AiAgentProperties.class);
                    assertEquals("mysql", contextProperties.getRag().getRegistry().getMode());
                    assertEquals("jdbc:mysql://127.0.0.1:3306/scm_ai_agent",
                            contextProperties.getRag().getRegistry().getMysql().getUrl());
                    assertEquals(1, context.getBeanNamesForType(MysqlRagDocumentRegistry.class).length);
                });
    }

    @Test
    void shouldCreateMysqlDataSourceOnlyWhenMysqlRegistryModeEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(MysqlDataSourceTestConfiguration.class)
                .withPropertyValues(
                        "ai.agent.rag.registry.mode=mysql",
                        "ai.agent.rag.registry.mysql.url=jdbc:mysql://127.0.0.1:3306/scm_ai_agent",
                        "ai.agent.rag.registry.mysql.username=root"
                )
                .run(context -> assertEquals(1, context.getBeanNamesForType(DataSource.class).length));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiAgentProperties.class)
    @Import(MysqlRagDocumentRegistry.class)
    static class MysqlRegistryTestConfiguration {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiAgentProperties.class)
    @Import(RagRegistryMysqlDataSourceConfiguration.class)
    static class MysqlDataSourceTestConfiguration {
    }
}
