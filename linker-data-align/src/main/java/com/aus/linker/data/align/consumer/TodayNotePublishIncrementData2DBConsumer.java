package com.aus.linker.data.align.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.data.align.constant.MQConstants;
import com.aus.linker.data.align.constant.RedisKeyConstants;
import com.aus.linker.data.align.constant.TableConstants;
import com.aus.linker.data.align.domain.mapper.InsertRecordMapper;
import com.aus.linker.data.align.model.dto.NoteOperateMqDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_data_align_" + MQConstants.TOPIC_NOTE_OPERATE,
    topic = MQConstants.TOPIC_NOTE_OPERATE
)
@Slf4j
public class TodayNotePublishIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private InsertRecordMapper insertRecordMapper;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【日增量数据入库：笔记发布数】，消息: {}", body);

        NoteOperateMqDTO noteOperateMqDTO = JsonUtil.parseObject(body, NoteOperateMqDTO.class);

        if (Objects.isNull(noteOperateMqDTO)) return;

        Long noteCreatorId = noteOperateMqDTO.getCreatorId();

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String bloomKey = RedisKeyConstants.buildBloomUserOperateListKey(date);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_user_note_publish_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), noteCreatorId);

        if (Objects.equals(0L, result)) {
            long userIdHashKey = noteCreatorId % tableShards;
            insertRecordMapper.insert2DataAlignUserNotePublishCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
        }

        RedisScript<Long> bloomScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        redisTemplate.execute(bloomScript, Collections.singletonList(bloomKey), noteCreatorId);

    }

}
