package com.aus.linker.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.domain.mapper.NoteCountDOMapper;
import com.aus.linker.count.biz.domain.mapper.UserCountDOMapper;
import com.aus.linker.count.biz.model.dto.AggregationCountCollectUnCollectNoteMqDTO;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB,
    topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB
)
@Slf4j
public class CountNoteCollect2DBConsumer implements RocketMQListener<String> {

    @Resource
    private NoteCountDOMapper noteCountDOMapper;

    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    private RateLimiter rateLimiter = RateLimiter.create(3000);

    @Override
    public void onMessage(String body) {
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数：笔记收藏数入库】, {}", body);

        List<AggregationCountCollectUnCollectNoteMqDTO> countList = null;
        try{
            countList = JsonUtil.parseList(body, AggregationCountCollectUnCollectNoteMqDTO.class);
        } catch (Exception e){
            log.error("## 解析 Json 字符串异常", e);
        }

        if (CollUtil.isNotEmpty(countList)) {
            countList.forEach(item -> {
                Long noteId = item.getNoteId();
                Long creatorId = item.getCreatorId();
                Integer count = item.getCount();

                transactionTemplate.execute(status -> {
                    try {
                        noteCountDOMapper.insertOrUpdateCollectTotalByNoteId(count, noteId);
                        userCountDOMapper.insertOrUpdateLikeTotalByUserId(count, creatorId);
                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly(); // 标记事务为回滚
                        log.error("", e);
                    }
                    return false;
                });
            });
        }

    }

}
