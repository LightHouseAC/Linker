package com.aus.linker.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.PageResponse;
import com.aus.framework.common.response.Response;
import com.aus.framework.common.utils.DateUtils;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.user.dto.resp.FindMultiUserByIdsRespDTO;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.aus.linker.user.relation.biz.constant.MQConstants;
import com.aus.linker.user.relation.biz.constant.RedisKeyConstants;
import com.aus.linker.user.relation.biz.domain.dataobject.FollowingDO;
import com.aus.linker.user.relation.biz.domain.service.FollowingService;
import com.aus.linker.user.relation.biz.enums.LuaResultEnum;
import com.aus.linker.user.relation.biz.enums.ResponseCodeEnum;
import com.aus.linker.user.relation.biz.model.dto.FollowUserMqDTO;
import com.aus.linker.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.aus.linker.user.relation.biz.model.vo.FindFollowingListReqVO;
import com.aus.linker.user.relation.biz.model.vo.FindFollowingListRespVO;
import com.aus.linker.user.relation.biz.model.vo.FollowUserReqVO;
import com.aus.linker.user.relation.biz.model.vo.UnfollowUserReqVO;
import com.aus.linker.user.relation.biz.rpc.UserRpcService;
import com.aus.linker.user.relation.biz.service.RelationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private FollowingService followingService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 关注的用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();

        // 当前登录的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 校验：无法关注自己
        if (Objects.equals(userId, followUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOURSELF);
        }

        // 校验关注用户是否存在
        FindUserByIdRespDTO findUserByIdRespDTO = userRpcService.findUserById(followUserId);

        if (Objects.isNull(findUserByIdRespDTO)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTS);
        }

        // 校验关注数是否已到上限
        // 构建当前用户关注列表的 Redis Key
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        // 当前时间转时间戳
        long timestamp = DateUtils.localDateTime2Timestamp(now);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), followUserId, timestamp);

        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);

        // ZSET 不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 从数据库查询当前用户的关注关系记录
            QueryWrapper<FollowingDO> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            List<FollowingDO> followingDOS = followingService.list(wrapper);

            // 随即过期时间，保底 1 天 + 随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            // 若记录为空，直接 ZADD 关系数据，并设置过期时间
            if (CollUtil.isEmpty(followingDOS)) {
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);
                // TODO: 根据用户类型，设置不同过期时间（区分 粉丝很多的用户 和 普通用户）

                redisTemplate.execute(script2, Collections.singletonList(followingRedisKey), followUserId, timestamp, expireSeconds);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本 follow_check_and_add.lua，将最新的关注关系添加进去
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), followUserId, timestamp);
                checkLuaScriptResult(result);
            }
        }
        // 发送 MQ
        // 构建消息体 DTO
        FollowUserMqDTO followUserMqDTO = FollowUserMqDTO.builder()
                .userId(userId)
                .followUserId(followUserId)
                .createTime(now)
                .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(followUserMqDTO)).build();

        // 通过冒号连接，可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

        log.info("==> 开始发送关注操作消息到 MQ, 消息体: {}", followUserMqDTO);

        // 一个用户可能短期内有高并发的被关注/被取关操作
        // 用被操作用户 ID 作为队列 Hash Key可能造成大量消息发进一个队列中，造成单点瓶颈
        // 因此使用操作发起人的 ID 作为 队列 Hash Key，一个用户同时能进行的关注/取关操作次数有限
        String hashKey = String.valueOf(userId);

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 消息发送到 MQ 成功， SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 消息发送到 MQ 异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
        // 待取关用户 ID
        Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 无法取关自己
        if (Objects.equals(userId, unfollowUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }

        // 校验关注的用户是否存在
        FindUserByIdRespDTO findUserByIdRespDTO = userRpcService.findUserById(unfollowUserId);
        if (Objects.isNull(findUserByIdRespDTO)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTS);
        }

        // 当前用户的关注列表 Redis Key
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(unfollowUserId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);

        // 校验 Lua 脚本返回结果
        // 取关的用户不在关注列表中
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
            throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }

        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) { // ZSET 关注列表不存在
            // 从数据库查询当前用户的关注关系记录
            QueryWrapper<FollowingDO> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            List<FollowingDO> followingDOS = followingService.list(wrapper);

            // 随即过期时间，保底 1 天 + 随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            // 若记录为空，则表示还未关注任何人，提示还未关注对方
            if (CollUtil.isEmpty(followingDOS)) {
                // TODO: 设置空 Redis 防止缓存穿透
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本: unfollow_check_and_delete.lua，将取关的用户删除
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);
                // 再次校验结果
                if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                    throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
                }
            }
        }

        // 发送 MQ 进行数据库操作
        // 构建消息体 DTO
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO.builder()
                .userId(userId)
                .unfollowUserId(unfollowUserId)
                .createTime(LocalDateTime.now())
                .build();

        // 构建消息对象，并将 DTO 转为 Json 字符串存入消息体
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(unfollowUserMqDTO)).build();

        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;

        log.info("==> 开始发送取关操作消息到 MQ, 消息体: {}", unfollowUserMqDTO);

        // 使用操作发起人的 ID 作为 队列 Hash Key
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 消息发送到 MQ 成功， SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 消息发送到 MQ 异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 查询关注列表
     * @param findFollowingListReqVO
     * @return
     */
    @Override
    public PageResponse<FindFollowingListRespVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {
        // 待查询用户 ID
        Long userId = findFollowingListReqVO.getUserId();
        // 页码
        Integer pageNo = findFollowingListReqVO.getPageNo();

        // 先从 Redis 中查询
        String followingListRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

        // 查询目标用户关注列表 ZSet 大小
        long total = redisTemplate.opsForZSet().zCard(followingListRedisKey);

        // 返参
        List<FindFollowingListRespVO> findFollowingListRespVOS = null;

        // 每页展示 10 条数据
        long limit = 10;

        if (total > 0) { // 缓存中有数据
            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(total, limit);

            // 请求的页码超出了总页数
            if (pageNo > totalPage) return PageResponse.success(null, pageNo, total);

            // 准备从 Redis 中查询 ZSet 分页数据
            // 每页 10 个元素，计算偏移量
            long offset = (pageNo - 1) * limit;

            // 使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
            // 使用 Double.NEGATIVE_INFINITY 和 Double.POSITIVE_INFINITY 作为分数范围 (-∞ ~ ＋∞)
            // 关注列表最多有 1000 个元素，可以确保获取到所有元素
            Set<Object> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(followingListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);

            if (CollUtil.isNotEmpty(followingUserIdsSet)) {
                // 提取所有用户 ID
                List<Long> userIds = followingUserIdsSet.stream().map(object -> Long.valueOf(object.toString())).toList();

                // RPC: 批量查询用户信息
                List<FindMultiUserByIdsRespDTO> findMultiUserByIdsRespDTOS = userRpcService.findByIds(userIds);

                // 若不为空，DTO 转 VO
                if (CollUtil.isNotEmpty(findMultiUserByIdsRespDTOS)) {
                    findFollowingListRespVOS = findMultiUserByIdsRespDTOS.stream()
                            .map(dto -> FindFollowingListRespVO.builder()
                                    .userId(dto.getId())
                                    .avatar(dto.getAvatar())
                                    .nickname(dto.getNickName())
                                    .introduction(dto.getIntroduction())
                                    .build())
                            .toList();
                }
            }
        } else {
            // 若 Redis 中没有数据，则从数据库中查询
            // 先查询记录总量
            QueryWrapper<FollowingDO> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            wrapper.orderBy(true, false, "create_time");
            long count = followingService.count(wrapper);

            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(count, limit);

            // 请求的页码超出了总页数
            if (pageNo > totalPage) return PageResponse.success(null, pageNo, count);

            // 分页查询
            Page<FollowingDO> page = new Page<>(pageNo, limit);
            List<FollowingDO> followingDOS = followingService.list(page, wrapper);

            // 若记录不为空
            if (CollUtil.isNotEmpty(followingDOS)) {
                // 提取所有关注用户 ID 到集合中
                List<Long> userIds = followingDOS.stream().map(FollowingDO::getFollowingUserId).toList();

                // RPC: 调用用户服务，将 DTO 转为 VO
                findFollowingListRespVOS = rpcUserServiceAndDTO2VO(userIds, findFollowingListRespVOS);

                // 异步将关注列表全量同步到 Redis
                threadPoolTaskExecutor.submit(() -> syncFollowingList2Redis(userId));
            }
        }
        return PageResponse.success(findFollowingListRespVOS, pageNo, total);
    }

    /**
     * 构建 Lua 脚本参数
     * @param followingDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int argsLength = followingDOS.size() * 2 +1;
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowingDO followingDO : followingDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(followingDO.getCreateTime()); // 关注时间作为 score
            luaArgs[i+1] = followingDO.getFollowingUserId(); // 关注的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
     * @param result
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回值错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    /**
     * RPC: 调用用户服务，将 DTO 转为 VO
     * @param userIds
     * @param findFollowingListRespVOS
     * @return
     */
    private List<FindFollowingListRespVO> rpcUserServiceAndDTO2VO(List<Long> userIds, List<FindFollowingListRespVO> findFollowingListRespVOS) {
        // RPC: 批量查询用户信息
        List<FindMultiUserByIdsRespDTO> findMultiUserByIdsRespDTOS = userRpcService.findByIds(userIds);

        // 若不为空，则 DTO 转为 VO
        if (CollUtil.isNotEmpty(findMultiUserByIdsRespDTOS)) {
            findFollowingListRespVOS = findMultiUserByIdsRespDTOS.stream()
                    .map(dto -> FindFollowingListRespVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .introduction(dto.getIntroduction())
                            .build())
                    .toList();
        }
        return findFollowingListRespVOS;
    }

    /**
     * 全量同步关注列表到 Redis 中
     * @param userId
     */
    private void syncFollowingList2Redis(Long userId) {
        // 查询全量关注用户列表（上限 1k）
        long limit = 1000;
        QueryWrapper<FollowingDO> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        Page<FollowingDO> page = new Page<>(1, limit);
        List<FollowingDO> followingDOS = followingService.list(page, wrapper);
        if (CollUtil.isNotEmpty(followingDOS)) {
            // 用户关注列表 Redis Key
            String followingListRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(followingListRedisKey), luaArgs);
        }
    }

}
