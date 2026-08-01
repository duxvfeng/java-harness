package com.chachamaru.harness.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Main Spring Boot Service entry point
 */
@SpringBootApplication
@MapperScan("com.chachamaru.harness.service.mapper")
public class HarnessService {

    public static void main(String[] args) {
        SpringApplication.run(HarnessService.class, args);
    }
}
