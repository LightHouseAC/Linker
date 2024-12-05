package com.aus.linker.data.align.consumer;

import com.aus.linker.data.align.constant.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_data_align" + MQConstants.TOPIC_COUNT_FOLLOWING,
    topic = MQConstants.TOPIC_COUNT_FOLLOWING
)
@Slf4j
public class TodayUserFollowIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【日增量数据入库：用户关注数】，消息: {}", body);

        
    }
}
