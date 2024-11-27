package com.aus.linker.data.align.consumer;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.data.align.constant.MQConstants;
import com.aus.linker.data.align.constant.RedisKeyConstants;
import com.aus.linker.data.align.constant.TableConstants;
import com.aus.linker.data.align.domain.mapper.InsertRecordMapper;
import com.aus.linker.data.align.model.dto.LikeUnlikeNoteMqDTO;
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
@RocketMQMessageListener(consumerGroup = "linker_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
    topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
@Slf4j
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private InsertRecordMapper insertRecordMapper;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【日增量数据入库：笔记点赞数】，消息: {}", body);

        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtil.parseObject(body, LikeUnlikeNoteMqDTO.class);

        if (Objects.isNull(likeUnlikeNoteMqDTO)) return;

        Long noteId = likeUnlikeNoteMqDTO.getNoteId();
        Long noteCreatorId = likeUnlikeNoteMqDTO.getNoteCreatorId();

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String bloomKey = RedisKeyConstants.buildBloomTodayNoteLikeListKey(date);

        // 1. 布隆过滤器判断该 日增量数据 是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_like_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), noteId);

        // 2. 若无，才会落库，减轻数据库压力（布隆过滤器判不存在，不会误判）
        if (Objects.equals(0L, result)) {
            long userIdHashKey = noteCreatorId % tableShards;
            long noteIdHashKey = noteId % tableShards;

            // 编程式事务保证多语句的原子性
            transactionTemplate.execute(status -> {
               try {
                   // 将日增量数据变更数据，分别写入 2 张临时表
                   // t_data_align_note_like_count_temp 和 t_data_align_user_like_count_temp
                   insertRecordMapper.insert2DataAlignNoteLikeCountTempTable(TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);
                   insertRecordMapper.insert2DataAlignUserLikeCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);

                   return true;
               } catch (Exception e) {
                   status.setRollbackOnly();
                   log.error("", e);
               }
               return false;
            });
        }
        // 3. 数据库写入成功后，再添加到布隆过滤器中
        RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        redisTemplate.execute(bloomAddScript, Collections.singletonList(bloomKey), noteId);
    }

}
