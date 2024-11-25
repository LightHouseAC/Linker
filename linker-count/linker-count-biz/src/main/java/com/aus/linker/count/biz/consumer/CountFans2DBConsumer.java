package com.aus.linker.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.domain.mapper.UserCountDOMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_FANS_2_DB,
    topic = MQConstants.TOPIC_COUNT_FANS_2_DB
)
@Slf4j
public class CountFans2DBConsumer implements RocketMQListener<String> {

    @Resource
    private UserCountDOMapper userCountDOMapper;

    // 每秒创建 3000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(3000);

    @Override
    public void onMessage(String body) {
        // 限流
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数：粉丝数入库】, {}...", body);

        Map<Long, Integer> countMap = null;
        try {
            countMap = JsonUtil.parseMap(body, Long.class, Integer.class);
        } catch (Exception e) {
            log.error("## 解析 Json 字符串异常", e);
        }

        if (CollUtil.isNotEmpty(countMap)) {
            // 数据库中若目标用户的记录已存在则更新否则插入
            countMap.forEach((k, v) -> userCountDOMapper.insertOrUpdateFansTotalByUserId(v, k));
        }
    }

}
