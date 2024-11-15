package com.aus.linker.user.relation.biz.consumer;

import com.aus.framework.common.utils.DateUtils;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.user.relation.biz.constant.MQConstants;
import com.aus.linker.user.relation.biz.constant.RedisKeyConstants;
import com.aus.linker.user.relation.biz.domain.dataobject.FansDO;
import com.aus.linker.user.relation.biz.domain.dataobject.FollowingDO;
import com.aus.linker.user.relation.biz.domain.service.FansService;
import com.aus.linker.user.relation.biz.domain.service.FollowingService;
import com.aus.linker.user.relation.biz.model.dto.FollowUserMqDTO;
import com.aus.linker.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "linker_group",    // Group
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,       // 消费的 Topic 主题
        consumeMode = ConsumeMode.ORDERLY                   // 消费模式: 顺序消费
)
public class FollowUnfollowConsumer implements RocketMQListener<Message> {

    @Resource
    private FollowingService followingService;

    @Resource
    private FansService fansService;

    @Resource
    private TransactionTemplate transactionTemplate;

    // 令牌桶：限制每秒处理的消息数
    @Resource
    private RateLimiter rateLimiter;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌实现，若没有令牌则阻塞等待令牌
        rateLimiter.acquire();

        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_FOLLOW)) { // 关注
            handleFollowTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) { // 取关
            handleUnfollowTagMessage(bodyJsonStr);
        }

    }

    /**
     * 取关
     * @param bodyJsonStr
     */
    private void handleUnfollowTagMessage(String bodyJsonStr) {
        // 将消息体 Json 字符串转为 DTO 对象
        UnfollowUserMqDTO unfollowUserMqDTO = JsonUtil.parseObject(bodyJsonStr, UnfollowUserMqDTO.class);

        // 判空
        if (Objects.isNull(unfollowUserMqDTO)) return;

        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();
        LocalDateTime createTime = unfollowUserMqDTO.getCreateTime();

        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 取关成功需要删除数据库2条记录
                // 关注表
                QueryWrapper<FollowingDO> wrapper = new QueryWrapper<>();
                wrapper.eq("user_id", userId);
                wrapper.eq("following_user_id", unfollowUserId);
                boolean result = followingService.remove(wrapper);
                if (result) {
                    QueryWrapper<FansDO> wrapper1 = new QueryWrapper<>();
                    wrapper1.eq("user_id", unfollowUserId);
                    wrapper1.eq("fans_user_id", userId);
                    result = fansService.remove(wrapper1);
                }
                return result;
            } catch (Exception e) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("", e);
            }
            return false;
        }));

        // 数据库删除成功后，更新 Redis，将自己从 Redis 的 ZSET 粉丝列表中移除
        if (isSuccess) {
            // 被取关用户的粉丝列表 Redis Key
            String fansRedisKey = RedisKeyConstants.buildUserFansKey(unfollowUserId);
            // 删除指定粉丝
            redisTemplate.opsForZSet().remove(fansRedisKey, userId);
        }

    }

    /**
     * 关注
     * @param bodyJsonStr
     */
    private void handleFollowTagMessage(String bodyJsonStr) {
        // 将消息体 Json 字符串转为 DTO 对象
        FollowUserMqDTO followUserMqDTO = JsonUtil.parseObject(bodyJsonStr, FollowUserMqDTO.class);

        // 判空
        if (Objects.isNull(followUserMqDTO)) return;

        // 幂等性：通过联合唯一索引保证

        Long userId = followUserMqDTO.getUserId();
        Long followUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();

        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 关注成功需往数据库添加两条记录
                // 关注表
                boolean result = followingService.save(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build()
                );
                // 粉丝表
                if (result) {
                    result = fansService.save(FansDO.builder()
                            .userId(followUserId)
                            .fansUserId(userId)
                            .createTime(createTime)
                            .build()
                    );
                }
                return result;
            } catch (Exception e) {
                status.setRollbackOnly();   // 标记事务为回滚
                log.error("", e);
            }
            return false;
        }));

        log.info("## 数据库添加记录结果：{}", isSuccess);
        // 若数据库操作成功, 更新 Redis 中被关注用户的 ZSet 粉丝列表

        if (isSuccess) {
            // Lua 脚本
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
            script.setResultType(Long.class);

            // 时间戳
            long timestamp = DateUtils.localDateTime2Timestamp(createTime);

            // 构建被关注用户的粉丝列表 Redis Key
            String fansRedisKey = RedisKeyConstants.buildUserFansKey(followUserId);
            // 执行脚本
            redisTemplate.execute(script, Collections.singletonList(fansRedisKey), userId, timestamp);
        }

    }

}
