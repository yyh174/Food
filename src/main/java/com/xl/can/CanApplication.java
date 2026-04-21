package com.xl.can;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.xl.can.mapper")
@EnableScheduling
public class CanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanApplication.class, args);
    }

}
