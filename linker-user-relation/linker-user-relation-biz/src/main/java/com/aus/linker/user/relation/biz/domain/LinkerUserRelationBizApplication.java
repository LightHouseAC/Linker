package com.aus.linker.user.relation.biz.domain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.aus.linker.user.relation.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.aus.linker")
public class LinkerUserRelationBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkerUserRelationBizApplication.class, args);
    }

}
