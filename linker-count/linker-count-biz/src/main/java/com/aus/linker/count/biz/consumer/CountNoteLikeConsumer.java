package com.aus.linker.count.biz.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.constant.RedisConstants;
import com.aus.linker.count.biz.enums.LikeUnlikeTypeEnum;
import com.aus.linker.count.biz.model.dto.CountLikeUnlikeNoteMqDTO;
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
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
    topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
@Slf4j
public class CountNoteLikeConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)                      // 缓存队列的最大容量
            .batchSize(1000)                        // 一次最多批量聚合 1000 条
            .linger(Duration.ofSeconds(1))          // 多久聚合一次
            .setConsumerEx(this::consumeMessage)    // 设置消费方法
            .build();

    @Override
    public void onMessage(String body) {
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【笔记点赞数】聚合消息, size: {}", bodys.size());
        log.info("==> 【笔记点赞数】聚合消息, {}", JsonUtil.toJsonString(bodys));

        // List<String> 转 List<CountLikeUnlikeMqDTO>
        List<CountLikeUnlikeNoteMqDTO> countLikeUnlikeNoteMqDTOS = bodys.stream()
                .map(body -> JsonUtil.parseObject(body, CountLikeUnlikeNoteMqDTO.class)).toList();

        // 按笔记 ID 进行分组
        Map<Long, List<CountLikeUnlikeNoteMqDTO>> groupMap = countLikeUnlikeNoteMqDTOS.stream()
                .collect(Collectors.groupingBy(CountLikeUnlikeNoteMqDTO::getNoteId));

        // 按组汇总数据，统计出最终的计数
        // key 为笔记 ID， value 为最终操作的计数
        Map<Long, Integer> countMap = Maps.newHashMap();

        for (Map.Entry<Long, List<CountLikeUnlikeNoteMqDTO>> entry : groupMap.entrySet()) {
            List<CountLikeUnlikeNoteMqDTO> list = entry.getValue();
            // 最终的计数值，默认为 0
            int finalCount = 0;
            for (CountLikeUnlikeNoteMqDTO countLikeUnlikeNoteMqDTO : list) {
                // 获取操作类型
                Integer type = countLikeUnlikeNoteMqDTO.getType();

                // 根据操作类型，获取对应枚举
                LikeUnlikeTypeEnum likeUnlikeTypeEnum = LikeUnlikeTypeEnum.valueOf(type);

                // 若枚举为空则忽略
                if (Objects.isNull(likeUnlikeTypeEnum)) continue;

                switch (likeUnlikeTypeEnum) {
                    case LIKE -> finalCount ++;
                    case UNLIKE -> finalCount --;
                }
            }
            countMap.put(entry.getKey(), finalCount);
        }

        log.info("## 【笔记点赞数】聚合后的计数数据: {}", JsonUtil.toJsonString(countMap));

        // 更新 Redis
        countMap.forEach((k, v) -> {
            // Redis Key
            String redisKey = RedisConstants.buildCountNoteKey(k);
            // 判断 Redis 中的 Hash 是否存在
            boolean isExists = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));

            // 若存在才会更新
            // 因为缓存有过期时间，过期后缓存会被删除，判断一下存在再更新，初始化工作放到查询计数的时候做
            if (isExists) {
                // 对目标用户 Hash 中的点赞数字段进行计数
                redisTemplate.opsForHash().increment(redisKey, RedisConstants.FIELD_FOLLOWING_TOTAL, v);
            }
        });

        // 发送 MQ，笔记点赞数据落库
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countMap)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记点赞数落库】MQ 消息发送成功, sendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【计数服务：笔记点赞数落库】MQ 消息发送异常: ", throwable);
            }
        });
    }

}