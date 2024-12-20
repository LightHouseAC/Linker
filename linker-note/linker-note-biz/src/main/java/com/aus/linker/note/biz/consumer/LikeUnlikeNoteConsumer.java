package com.aus.linker.note.biz.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.note.biz.constant.MQConstants;
import com.aus.linker.note.biz.domain.dataobject.NoteLikeDO;
import com.aus.linker.note.biz.domain.mapper.NoteLikeDOMapper;
import com.aus.linker.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
    topic = MQConstants.TOPIC_LIKE_OR_UNLIKE,
    consumeMode = ConsumeMode.ORDERLY
)
@Slf4j
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {

    private RateLimiter rateLimiter = RateLimiter.create(3000);

    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(Message message) {
        // 限流
        rateLimiter.acquire();

        // 幂等性：通过联合唯一索引保证

        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("## 消费到了 MQ 消息【笔记：点赞/取消点赞】: {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_LIKE)) { // 点赞笔记
            handleLikeNoteTagMessage(bodyJsonStr);
        } else if(Objects.equals(tags, MQConstants.TAG_UNLIKE)) { // 取消点赞笔记
            handleUnlikeNoteTagMessage(bodyJsonStr);
        }
    }

    /**
     * 笔记取消点赞
     * @param bodyJsonStr
     */
    private void handleUnlikeNoteTagMessage(String bodyJsonStr) {
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtil.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);

        if (Objects.isNull(likeUnlikeNoteMqDTO)) return;

        Long userId = likeUnlikeNoteMqDTO.getUserId();
        Long noteId = likeUnlikeNoteMqDTO.getNoteId();
        Integer type = likeUnlikeNoteMqDTO.getType();
        LocalDateTime createTime = likeUnlikeNoteMqDTO.getCreateTime();

        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(userId)
                .noteId(noteId)
                .createTime(createTime)
                .status(type)
                .build();

        int count = noteLikeDOMapper.update2UnlikeByUserIdAndNoteId(noteLikeDO);

        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数：笔记点赞】 MQ 消息发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【计数：笔记点赞】 MQ 消息发送异常: ", throwable);
            }
        });

    }

    /**
     * 笔记点赞
     * @param bodyJsonStr
     */
    private void handleLikeNoteTagMessage(String bodyJsonStr) {
        // 消息体 Json 字符串 转 DTO
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtil.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);

        if (Objects.isNull(likeUnlikeNoteMqDTO)) return;

        Long userId = likeUnlikeNoteMqDTO.getUserId();
        Long noteId = likeUnlikeNoteMqDTO.getNoteId();
        Integer type = likeUnlikeNoteMqDTO.getType();
        LocalDateTime createTime = likeUnlikeNoteMqDTO.getCreateTime();

        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .noteId(noteId)
                .userId(userId)
                .createTime(createTime)
                .status(type)
                .build();

        // 添加或更新笔记点赞记录
        int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);

        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数：笔记点赞】 MQ 消息发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【计数：笔记点赞】 MQ 消息发送异常: ", throwable);
            }
        });

    }
}


