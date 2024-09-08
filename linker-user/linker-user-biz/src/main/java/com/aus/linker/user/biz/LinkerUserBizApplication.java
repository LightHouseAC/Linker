package com.aus.linker.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.aus.linker.user.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.aus.linker")
public class LinkerUserBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkerUserBizApplication.class, args);
    }

}
