package com.aus.linker.note.biz.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.note.biz.constant.MQConstants;
import com.aus.linker.note.biz.domain.dataobject.NoteCollectionDO;
import com.aus.linker.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.aus.linker.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
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

    @Resource
    private RocketMQTemplate rocketMQTemplate;

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

        int count = noteCollectionDOMapper.update2UnCollectByUserIdAndNoteId(noteCollectionDO);

        if (count == 0) return;

        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr)
                .build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记收藏】MQ 消息发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记收藏】MQ 消息发送异常: ", throwable);
            }
        });
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

        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr)
                .build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记收藏】MQ 消息发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记收藏】MQ 消息发送异常: ", throwable);
            }
        });
    }


}
