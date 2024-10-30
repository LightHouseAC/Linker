package com.aus.linker.note.biz.consumer;

import com.aus.linker.note.biz.constant.MQConstants;
import com.aus.linker.note.biz.domain.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "linker_group", // group
        topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, // 消费主题 Topic
        messageModel = MessageModel.BROADCASTING // 广播模式
)
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {

    @Resource
    private NoteService noteService;

    @Override
    public void onMessage(String body) {
        Long noteId = Long.valueOf(body);
        log.info("## 消费者消费成功, noteId: {}", noteId);

        noteService.deleteNoteLocalCache(noteId);
    }

}
