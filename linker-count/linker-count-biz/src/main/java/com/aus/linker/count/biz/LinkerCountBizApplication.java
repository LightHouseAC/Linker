package com.aus.linker.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aus.linker.count.biz.domain.mapper")
public class LinkerCountBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkerCountBizApplication.class, args);
    }

}
