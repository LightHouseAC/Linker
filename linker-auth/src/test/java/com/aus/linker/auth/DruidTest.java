package com.aus.linker.auth;

import com.alibaba.druid.filter.config.ConfigTools;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class DruidTest {

    /**
     * Druid 密码加密
     */
    @Test
    @SneakyThrows
    void test(){
        String passwd = "@Linker10086";
        String[] arr = ConfigTools.genKeyPair(512);
        // 私钥
        log.info("privateKey: {}", arr[0]);
        // 公钥
        log.info("publicKey: {}", arr[1]);
        // 私钥加密密码
        String encodePassword = ConfigTools.encrypt(arr[0], passwd);
        log.info("password: {}", encodePassword);
    }

}
