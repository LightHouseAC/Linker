package com.aus.linker.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.aus.linker.auth.domain.mapper")
public class LinkerAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkerAuthApplication.class, args);
    }

}
