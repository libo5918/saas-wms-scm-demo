package com.example.scm.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.scm")
public class ScmInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmInventoryApplication.class, args);
    }
}
