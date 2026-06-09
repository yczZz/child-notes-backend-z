package com.ycz.childnotesbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ycz.childnotesbackend.mapper")
@SpringBootApplication
public class ChildNotesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChildNotesBackendApplication.class, args);
    }
}
