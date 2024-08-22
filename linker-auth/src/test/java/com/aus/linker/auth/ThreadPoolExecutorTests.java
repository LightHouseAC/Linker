package com.aus.linker.auth;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;

@SpringBootTest
@Slf4j
public class ThreadPoolExecutorTests {

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 测试线程池
     */
    @Test
    void testSubmit(){
        threadPoolTaskExecutor.submit(()->{
           log.info("异步线程调用测试：ok");
        });
    }

}