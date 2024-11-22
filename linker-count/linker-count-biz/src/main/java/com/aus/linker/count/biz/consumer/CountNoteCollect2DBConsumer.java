package com.aus.linker.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.domain.mapper.NoteCountDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB,
    topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB
)
@Slf4j
public class CountNoteCollect2DBConsumer implements RocketMQListener<String> {

    @Resource
    private NoteCountDOMapper noteCountDOMapper;

    private RateLimiter rateLimiter = RateLimiter.create(3000);

    @Override
    public void onMessage(String body) {
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数：笔记收藏数入库】, {}", body);

        Map<Long, Integer> countMap = null;

        try{
            countMap = JsonUtil.parseMap(body, Long.class, Integer.class);
        } catch (Exception e){
            log.error("## 解析 Json 字符串异常", e);
        }

        if (CollUtil.isNotEmpty(countMap)) {
            countMap.forEach((k, v) -> noteCountDOMapper.insertOrUpdateCollectTotalByNoteId(v, k));
        }

    }

}
