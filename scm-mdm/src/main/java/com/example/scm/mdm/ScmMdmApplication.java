package com.example.scm.mdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.scm")
public class ScmMdmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmMdmApplication.class, args);
    }
}
