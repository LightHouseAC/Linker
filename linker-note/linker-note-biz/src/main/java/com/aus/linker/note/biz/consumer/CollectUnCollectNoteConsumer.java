package com.aus.linker.note.biz.consumer;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.note.biz.constant.MQConstants;
import com.aus.linker.note.biz.domain.dataobject.NoteCollectionDO;
import com.aus.linker.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.aus.linker.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_COLLECT_OR_UN_COLLECT,
    topic = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT,
        consumeMode = ConsumeMode.ORDERLY
)
@Slf4j
public class CollectUnCollectNoteConsumer implements RocketMQListener<Message> {

    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

    private RateLimiter rateLimiter = RateLimiter.create(3000);

    public CollectUnCollectNoteConsumer(NoteCollectionDOMapper noteCollectionDOMapper) {
        this.noteCollectionDOMapper = noteCollectionDOMapper;
    }

    @Override
    public void onMessage(Message message) {
        rateLimiter.acquire();

        // 幂等性：通过唯一联合索引保证

        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("## 消费到了 MQ 消息【笔记：收藏/取消收藏】: {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_COLLECT)) {
            handleCollectNoteTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UN_COLLECT)) {
            handleUnCollectNoteTagMessage(bodyJsonStr);
        }
    }

    /**
     * 笔记取消收藏
     * @param bodyJsonStr
     */
    private void handleUnCollectNoteTagMessage(String bodyJsonStr) {

    }

    /**
     * 笔记收藏
     * @param bodyJsonStr
     */
    private void handleCollectNoteTagMessage(String bodyJsonStr) {
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = JsonUtil.parseObject(bodyJsonStr, CollectUnCollectNoteMqDTO.class);

        if (Objects.isNull(collectUnCollectNoteMqDTO)) return;

        Long userId = collectUnCollectNoteMqDTO.getUserId();
        Long noteId = collectUnCollectNoteMqDTO.getNoteId();
        Integer type = collectUnCollectNoteMqDTO.getType();
        LocalDateTime createTime = collectUnCollectNoteMqDTO.getCreateTime();

        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(userId)
                .noteId(noteId)
                .status(type)
                .createTime(createTime)
                .build();

        int count = noteCollectionDOMapper.insertOrUpdate(noteCollectionDO);

        // TODO：发送计数 MQ

    }


}
