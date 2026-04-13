package com.example.scm.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.scm")
public class ScmPurchaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmPurchaseApplication.class, args);
    }
}
