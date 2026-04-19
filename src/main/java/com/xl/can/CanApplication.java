package com.xl.can;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xl.can.mapper")
public class CanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanApplication.class, args);
    }

}
