package com.flowwork;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.flowwork.modules.**.mapper")
@EnableScheduling
public class FlowWorkApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowWorkApplication.class, args);
    }
}
