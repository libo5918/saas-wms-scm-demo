package com.example.scm.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.scm")
@EnableScheduling
public class ScmSalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmSalesApplication.class, args);
    }
}
