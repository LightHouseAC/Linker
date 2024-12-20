package com.aus.linker.user.relation.biz;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.user.relation.biz.constant.MQConstants;
import com.aus.linker.user.relation.biz.enums.FollowUnfollowTypeEnum;
import com.aus.linker.user.relation.biz.model.dto.CountFollowUnfollowMqDTO;
import com.aus.linker.user.relation.biz.model.dto.FollowUserMqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@SpringBootTest
@Slf4j
public class MQTests {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 测试：发送 1w 条消息测试令牌桶限流器
     */
    @Test
    void testBatchSendMQ() {
        for (long i = 0; i < 10000; i++) {
            // 构建消息体 DTO
            FollowUserMqDTO followUserMqDTO = FollowUserMqDTO.builder()
                    .userId(i)
                    .followUserId(i)
                    .createTime(LocalDateTime.now())
                    .build();

            // 构建消息对象，将 DTO 转成 Json 字符串设置到消息体中
            Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(followUserMqDTO)).build();

            String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

            log.info("==> 开始发送关注 MQ, 消息体: {}", followUserMqDTO);

            rocketMQTemplate.asyncSend(destination, message, new SendCallback() {

                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==> MQ 发送成功，SendResult: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("==> MQ 发送异常: ", throwable);
                }
            });

        }
    }

    /**
     * 测试：发送计数 MQ 消息，统计粉丝数
     */
    @Test
    void testSendCountFollowUnfollowMQ() {
        // 循环发送 3200 条 MQ
        for (long i = 0; i < 3200; i++) {
            // 构建消息体 DTO
            CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(i + 1)
                    .targetUserId(26L)
                    .type(FollowUnfollowTypeEnum.FOLLOW.getCode())
                    .build();

            Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countFollowUnfollowMqDTO)).build();

            rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
                }
            });

            rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==> 【计数服务：关注数】MQ 发送成功，SendResult: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("==> 【计数服务：关注数】MQ 发送异常: ", throwable);
                }
            });

            // 省略...

        }
    }

}
