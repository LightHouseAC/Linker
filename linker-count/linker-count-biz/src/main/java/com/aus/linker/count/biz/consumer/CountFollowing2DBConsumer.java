package com.aus.linker.count.biz.consumer;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.domain.mapper.UserCountDOMapper;
import com.aus.linker.count.biz.enums.FollowUnfollowTypeEnum;
import com.aus.linker.count.biz.model.dto.CountFollowUnfollowMqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_FOLLOWING_2_DB,
    topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB
)
@Slf4j
public class CountFollowing2DBConsumer implements RocketMQListener<String> {

    @Resource
    private UserCountDOMapper userCountDOMapper;

    private RateLimiter rateLimiter = RateLimiter.create(3000);

    @Override
    public void onMessage(String body) {
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 消息 【计数：关注数入库】, {}", body);

        if (StringUtils.isBlank(body)) return;

        CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = JsonUtil.parseObject(body, CountFollowUnfollowMqDTO.class);

        Integer type = countFollowUnfollowMqDTO.getType();
        Long userId = countFollowUnfollowMqDTO.getUserId();

        int count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;

        userCountDOMapper.insertOrUpdateFollowingTotalByUserId(count, userId);
    }
}
