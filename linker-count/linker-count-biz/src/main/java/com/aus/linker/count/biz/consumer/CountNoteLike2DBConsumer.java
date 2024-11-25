package com.aus.linker.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.domain.mapper.NoteCountDOMapper;
import com.aus.linker.count.biz.domain.mapper.UserCountDOMapper;
import com.aus.linker.count.biz.model.dto.AggregationCountLikeUnlikeNoteMqDTO;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB,
    topic = MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB
)
@Slf4j
public class CountNoteLike2DBConsumer implements RocketMQListener<String> {

    @Resource
    private NoteCountDOMapper noteCountDOMapper;

    private RateLimiter rateLimiter = RateLimiter.create(3000);

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(String body) {
        // 限流
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数：笔记点赞数入库】, {}", body);

        List<AggregationCountLikeUnlikeNoteMqDTO> countList = null;
        try {
            countList = JsonUtil.parseList(body, AggregationCountLikeUnlikeNoteMqDTO.class);
        } catch (Exception e) {
            log.error("## 解析 JSON 字符串异常", e);
        }

        if (CollUtil.isNotEmpty(countList)) {
            // 判断数据库中的 t_note_count 表，若笔记计数记录不存在，则插入；若记录已存在，则直接更新
            countList.forEach(item -> {
                Long creatorId = item.getCreatorId();
                Long noteId = item.getNoteId();
                Integer count = item.getCount();

                // 编程式事务，保证 2 条语句的原子性
                transactionTemplate.execute(status -> {
                    try {
                        noteCountDOMapper.insertOrUpdateLikeTotalByNoteId(count, noteId);
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
