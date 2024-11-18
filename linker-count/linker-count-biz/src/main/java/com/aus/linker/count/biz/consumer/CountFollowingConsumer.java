package com.aus.linker.count.biz.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.constant.RedisConstants;
import com.aus.linker.count.biz.enums.FollowUnfollowTypeEnum;
import com.aus.linker.count.biz.model.dto.CountFollowUnfollowMqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING // 主题 Topic
)
@Slf4j
public class CountFollowingConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 消息【计数：关注数】, {}", body);

        if (StringUtils.isBlank(body)) return;

        CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = JsonUtil.parseObject(body, CountFollowUnfollowMqDTO.class);

        Integer type = countFollowUnfollowMqDTO.getType();
        Long userId = countFollowUnfollowMqDTO.getUserId();

        String redisKey = RedisConstants.buildCountUserKey(userId);
        boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));

        if (exists) {
            long count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
            redisTemplate.opsForHash().increment(redisKey, RedisConstants.FIELD_FOLLOWING_TOTAL, count);
        }

        // 发送 MQ，关注数写库
        // 构建消息对象
        Message<String> message = MessageBuilder.withPayload(body).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数入库】 MQ 消息发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【计数服务：关注数入库】 MQ 消息发送异常: ", throwable);
            }
        });

    }
}
