package com.aus.linker.data.align.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {

}
