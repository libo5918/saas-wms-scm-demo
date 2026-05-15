package com.example.scm.aiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.example.scm")
@EnableConfigurationProperties(AiAgentProperties.class)
public class ScmAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmAiAgentApplication.class, args);
    }
}
