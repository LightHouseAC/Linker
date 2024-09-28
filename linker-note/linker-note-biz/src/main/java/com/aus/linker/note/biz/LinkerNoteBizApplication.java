package com.aus.linker.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.aus.linker.note.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.aus.linker")
public class LinkerNoteBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkerNoteBizApplication.class, args);
    }

}