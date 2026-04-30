package com.example.scm.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ScmAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmAuthApplication.class, args);
    }
}
