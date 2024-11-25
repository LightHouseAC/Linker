package com.aus.linker.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aus.linker.data.align.domain.mapper")
public class LinkerDataAlignApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkerDataAlignApplication.class, args);
    }

}