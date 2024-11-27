package com.aus.linker.data.align.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.data.align.constant.MQConstants;
import com.aus.linker.data.align.constant.RedisKeyConstants;
import com.aus.linker.data.align.constant.TableConstants;
import com.aus.linker.data.align.domain.mapper.InsertRecordMapper;
import com.aus.linker.data.align.model.dto.CollectUnCollectMqDTO;
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
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "linker_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
    topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT
)
@Slf4j
public class TodayNoteCollectIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private InsertRecordMapper insertRecordMapper;

    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【日增量数据入库：笔记收藏数】，消息: {}", body);

        CollectUnCollectMqDTO collectUnCollectMqDTO = JsonUtil.parseObject(body, CollectUnCollectMqDTO.class);

        if (Objects.isNull(collectUnCollectMqDTO)) return;

        Long noteId = collectUnCollectMqDTO.getNoteId();
        Long noteCreatorId = collectUnCollectMqDTO.getNoteCreatorId();

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String bloomKey = RedisKeyConstants.buildBloomTodayNoteCollectListKey(date);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_collect_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), noteId);

        if (Objects.equals(0L, result)) {
            long userIdHashKey = noteCreatorId % tableShards;
            long noteIdHashKey = noteId % tableShards;

            transactionTemplate.execute(status -> {
                try {
                    insertRecordMapper.insert2DataAlignNoteCollectCountTempTable(TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);
                    insertRecordMapper.insert2DataAlignUserCollectCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    log.error("", e);
                }
                return false;
            });

            RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
            redisTemplate.execute(bloomAddScript, Collections.singletonList(bloomKey), noteId);
        }

    }

}
