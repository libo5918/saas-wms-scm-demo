package com.example.scm.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.scm")
public class ScmSalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmSalesApplication.class, args);
    }
}
