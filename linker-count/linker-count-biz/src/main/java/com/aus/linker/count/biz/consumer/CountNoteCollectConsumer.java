package com.aus.linker.count.biz.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.constant.RedisConstants;
import com.aus.linker.count.biz.enums.CollectUnCollectNoteTypeEnum;
import com.aus.linker.count.biz.model.dto.CountCollectUnCollectNoteMqDTO;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
    topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT
)
@Slf4j
public class CountNoteCollectConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(500000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();

    @Override
    public void onMessage(String body) {
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【笔记收藏数】聚合消息, size: {}", bodys.size());
        log.info("==> 【笔记收藏数】聚合消息, {}", JsonUtil.toJsonString(bodys));

        List<CountCollectUnCollectNoteMqDTO> countCollectUnCollectNoteMqDTOS = bodys.stream()
                .map(body -> JsonUtil.parseObject(body, CountCollectUnCollectNoteMqDTO.class)).toList();

        // 按笔记 ID 进行分组
        Map<Long, List<CountCollectUnCollectNoteMqDTO>> groupMap = countCollectUnCollectNoteMqDTOS.stream()
                .collect(Collectors.groupingBy(CountCollectUnCollectNoteMqDTO::getNoteId));

        // 按组汇总数据，统计出最终的计数
        Map<Long, Integer> countMap = Maps.newHashMap();

        for(Map.Entry<Long, List<CountCollectUnCollectNoteMqDTO>> entry : groupMap.entrySet()) {
            List<CountCollectUnCollectNoteMqDTO> list = entry.getValue();
            int finalCount = 0;
            for (CountCollectUnCollectNoteMqDTO countCollectUnCollectNoteMqDTO : list) {
                Integer type = countCollectUnCollectNoteMqDTO.getType();

                CollectUnCollectNoteTypeEnum collectUnCollectNoteTypeEnum = CollectUnCollectNoteTypeEnum.valueOf(type);

                if (Objects.isNull(collectUnCollectNoteTypeEnum)) continue;

                switch (collectUnCollectNoteTypeEnum) {
                    case COLLECT -> finalCount++;
                    case UN_COLLECT -> finalCount--;
                }
            }
            countMap.put(entry.getKey(), finalCount);
        }

        log.info("## 【笔记收藏数】聚合后的计数数据: {}", JsonUtil.toJsonString(countMap));

        countMap.forEach((k, v) -> {
            String redisKey = RedisConstants.buildCountNoteKey(k);
            boolean isExists = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));

            if (isExists) {
                redisTemplate.opsForHash().increment(redisKey, RedisConstants.FIELD_COLLECT_TOTAL, v);
            }
        });

        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countMap)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记收藏数入库】 MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【计数服务：笔记收藏数入库】 MQ 发送异常: ", throwable);
            }
        });

    }

}