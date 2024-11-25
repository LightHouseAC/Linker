package com.aus.linker.count.biz.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.count.biz.constant.MQConstants;
import com.aus.linker.count.biz.constant.RedisConstants;
import com.aus.linker.count.biz.domain.mapper.UserCountDOMapper;
import com.aus.linker.count.biz.model.dto.NoteOperateMqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup =  "linker_group_" + MQConstants.TOPIC_NOTE_OPERATE,
    topic = MQConstants.TOPIC_NOTE_OPERATE
)
@Slf4j
public class CountNotePublishConsumer implements RocketMQListener<Message> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(Message message) {
        String bodyJsonStr = new String(message.getBody());
        String tags = message.getTags();

        log.info("## 消费到了 MQ 消息【计数：笔记发布数】, body: {}, tags: {}", bodyJsonStr, tags);

        if (Objects.equals(tags, MQConstants.TAG_NOTE_PUBLISH)) {
            handleTagMessage(bodyJsonStr, 1);
        } else if (Objects.equals(tags, MQConstants.TAG_NOTE_DELETE)) {
            handleTagMessage(bodyJsonStr, -1);
        }
    }

    private void handleTagMessage(String bodyJsonStr, int count) {
        NoteOperateMqDTO noteOperateMqDTO = JsonUtil.parseObject(bodyJsonStr, NoteOperateMqDTO.class);
        if (Objects.isNull(noteOperateMqDTO)) return;
        Long creatorId = noteOperateMqDTO.getCreatorId();

        String countUserRedisKey = RedisConstants.buildCountUserKey(creatorId);
        boolean isExists = Boolean.TRUE.equals(redisTemplate.hasKey(countUserRedisKey));
        if (isExists) {
            redisTemplate.opsForHash().increment(countUserRedisKey, RedisConstants.FIELD_NOTE_TOTAL, count);
        }
        userCountDOMapper.insertOrUpdateNoteTotalByUserId(count, creatorId);
    }

}
