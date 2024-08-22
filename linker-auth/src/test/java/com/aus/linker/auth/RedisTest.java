package com.aus.linker.auth;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
@Slf4j
public class RedisTest {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testSetKeyValue() {
        // 添加一个key为name，value值为 test_value
        redisTemplate.opsForValue().set("name", "test_value");
    }

    @Test
    void testHasKey() {
        log.info("key 是否存在: {}", Boolean.TRUE.equals(redisTemplate.hasKey("name")));
    }

    @Test
    void testGetValue() {
        log.info("value 值:{}", redisTemplate.opsForValue().get("name"));
    }

    @Test
    void testDelete() {
        redisTemplate.delete("name");
    }

}
