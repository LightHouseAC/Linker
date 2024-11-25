package com.aus.linker.note.biz.domain.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.framework.common.utils.DateUtils;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.note.biz.constant.MQConstants;
import com.aus.linker.note.biz.constant.RedisKeyConstants;
import com.aus.linker.note.biz.domain.dataobject.NoteCollectionDO;
import com.aus.linker.note.biz.domain.dataobject.NoteDO;
import com.aus.linker.note.biz.domain.dataobject.NoteLikeDO;
import com.aus.linker.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.aus.linker.note.biz.domain.mapper.NoteDOMapper;
import com.aus.linker.note.biz.domain.mapper.NoteLikeDOMapper;
import com.aus.linker.note.biz.domain.mapper.TopicDOMapper;
import com.aus.linker.note.biz.domain.service.NoteService;
import com.aus.linker.note.biz.enums.*;
import com.aus.linker.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import com.aus.linker.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.aus.linker.note.biz.model.vo.*;
import com.aus.linker.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.aus.linker.note.biz.rpc.KeyValueRpcService;
import com.aus.linker.note.biz.rpc.UserRpcService;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
* @author recww
* @description 针对表【t_note(笔记表)】的数据库操作Service实现
* @createDate 2024-09-24 00:14:08
*/
@Service
@Slf4j
public class NoteServiceImpl extends ServiceImpl<NoteDOMapper, NoteDO>
    implements NoteService {

    @Resource
    private TopicDOMapper topicDOMapper;

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private KeyValueRpcService keyValueRpcService;

    @Resource
    private UserRpcService userRpcService;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;

    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

    @Resource
    private NoteDOMapper noteDOMapper;

    /**
     * 笔记发布
     * @param publishNoteReqVO
     * @return
     */
    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        // 笔记类型
        Integer type = publishNoteReqVO.getType();

        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 如果不是图文、视频类型，则抛出业务异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        // 笔记内容是否为空，默认值为true
        Boolean isContentEmpty = true;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT: // 图文笔记
                List<String> imgUriList = publishNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 9, "笔记图片不能多于 9 张");
                // 将图片链接拼接，用逗号分隔
                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO: // 视频笔记
                videoUri = publishNoteReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
            default:
                break;
        }

        // RPC: 调用分布式 ID 生成服务，生成笔记 ID
        String snowflakeId = distributedIdGeneratorRpcService.getSnowflakeId();
        // 笔记内容 UUID
        String contentUuid = null;

        // 笔记内容
        String content = publishNoteReqVO.getContent();

        // 若用户填写了笔记内容
        if (StringUtils.isNotBlank(content)) {
            // 内容是否为空 标记 置否
            isContentEmpty = false;
            // 生成笔记内容 UUID
            contentUuid = UUID.randomUUID().toString();
            // RPC: 调用 KV 键值服务，存储短文本
            boolean isSavedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);

            // 若存储失败，抛出业务异常
            if (!isSavedSuccess) {
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        // 话题
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectById(topicId).getName();

            // 判断提交的话题是否存在
            if (StringUtils.isBlank(topicName)) throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
        }

        // 发布者用户 ID
        Long creatorId = LoginUserContextHolder.getUserId();

        // 构建笔记 DO 对象
        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowflakeId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUris(imgUris)
                .title(publishNoteReqVO.getTitle())
                .topicId(topicId)
                .topicName(topicName)
                .type(type)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .isTop(Boolean.FALSE)
                .videoUri(videoUri)
                .contentUuid(contentUuid)
                .build();

        try { // 笔记入库存储
            save(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记存储失败", e);

            // RPC: 笔记保存失败，则删除笔记内容
            if (StringUtils.isNotBlank(contentUuid)) {
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }

        return Response.success();
    }

    @Override
    @SneakyThrows
    public Response<FindNoteDetailRespVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
        // 查询的笔记 ID
        Long noteId = findNoteDetailReqVO.getId();

        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 先从本地缓存中查询
        String findNoteDetailRespVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        if (StringUtils.isNotBlank(findNoteDetailRespVOStrLocalCache)) {
            FindNoteDetailRespVO findNoteDetailRespVO = JsonUtil.parseObject(findNoteDetailRespVOStrLocalCache, FindNoteDetailRespVO.class);
            log.info("==> 命中了本地缓存: {}", findNoteDetailRespVOStrLocalCache);
            // 可见性校验
            checkNoteVisibleFromVO(userId, findNoteDetailRespVO);
            return Response.success(findNoteDetailRespVO);
        }

        // 从 Redis 缓存中获取
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

        // 若缓存中有该笔记数据则直接返回
        if (StringUtils.isNotBlank(noteDetailJson)) {
            FindNoteDetailRespVO findNoteDetailRespVO = JsonUtil.parseObject(noteDetailJson, FindNoteDetailRespVO.class);

            // 异步线程将笔记数据写入本地缓存
            threadPoolTaskExecutor.submit(() -> {
                // 写入本地缓存
                LOCAL_CACHE.put(noteId,
                        Objects.isNull(findNoteDetailRespVO) ? "null" : JsonUtil.toJsonString(findNoteDetailRespVO));
            });

            // 可见性校验
            if (Objects.nonNull(findNoteDetailRespVO)) {
                Integer visible = findNoteDetailRespVO.getVisible();
                checkNoteVisible(visible, userId, findNoteDetailRespVO.getCreatorId());
            }
            return Response.success(findNoteDetailRespVO);
        }

        // 若 Redis 缓存中获取不到，则走 数据库查询
        // 查询笔记
        QueryWrapper<NoteDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", noteId).eq("status", 1);
        NoteDO noteDO = getOne(wrapper);

        // 若笔记不存在，抛出业务异常
        if (Objects.isNull(noteDO)) {
            threadPoolTaskExecutor.submit(() -> {
                // 防止缓存穿透，将空数据写入 Redis缓存
                // 过期时间保底 1min + 随机s，防止缓存雪崩
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(noteDetailRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 可见性校验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());

        // 并发查询优化：使用CompletableFuture

        // RPC: 调用用户服务 获取 发布者详情
        Long creatorId = noteDO.getCreatorId();
        CompletableFuture<FindUserByIdRespDTO> userResultFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.findById(creatorId), threadPoolTaskExecutor);
        // RPC: 调用 KV 存储服务获取笔记内容
        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.findNoteContent(noteDO.getContentUuid()), threadPoolTaskExecutor);
        }
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindNoteDetailRespVO> resultFuture = CompletableFuture
                .allOf(userResultFuture, contentResultFuture)
                .thenApply(s -> {
                    // 获取 Future 返回的结果
                    FindUserByIdRespDTO findUserByIdRespDTO = userResultFuture.join();
                    String content = finalContentResultFuture.join();

                    // 笔记类型
                    Integer type = noteDO.getType();
                    // 图文笔记图片链接 字符串
                    String imgUrisStr = noteDO.getImgUris();
                    // 图文笔记图片链接 列表
                    List<String> imgUris = null;
                    // 如果查询的是图文笔记，则将图片链接 通过逗号分隔转换成列表
                    if (Objects.equals(type, NoteTypeEnum.IMAGE_TEXT.getCode())
                            && StringUtils.isNotBlank(imgUrisStr)) {
                        imgUris = List.of(imgUrisStr.split(","));
                    }

                    // 构建返参 VO 实体类
                    return FindNoteDetailRespVO.builder()
                            .id(noteDO.getId())
                            .type(noteDO.getType())
                            .title(noteDO.getTitle())
                            .content(content)
                            .imgUris(imgUris)
                            .topicId(noteDO.getTopicId())
                            .creatorId(creatorId)
                            .creatorName(findUserByIdRespDTO.getNickName())
                            .avatar(findUserByIdRespDTO.getAvatar())
                            .videoUri(noteDO.getVideoUri())
                            .updateTime(noteDO.getUpdateTime())
                            .visible(noteDO.getVisible())
                            .build();

                });

        // 获取拼装后的 FindNoteDetailRespVO
        FindNoteDetailRespVO findNoteDetailRespVO = resultFuture.get();

        // 异步线程将笔记详情存入 Redis
        threadPoolTaskExecutor.submit(() -> {
            String noteDetailJson1 = JsonUtil.toJsonString(findNoteDetailRespVO);
            // 过期时间（保底1天 + 随机秒数，防止雪崩）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(noteDetailRedisKey, noteDetailJson1, expireSeconds, TimeUnit.SECONDS);
        });

        return Response.success(findNoteDetailRespVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
        // 笔记ID
        Long noteId = updateNoteReqVO.getId();
        // 笔记类型
        Integer type = updateNoteReqVO.getType();

        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 若非图文、视频笔记，抛出业务异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum){
            case IMAGE_TEXT:    // 图文笔记
                List<String> imgUriList = updateNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 9, "笔记图片不能多于9张");

                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO:
                videoUri = updateNoteReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
            default:
                break;
        }

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();
        NoteDO selectNoteDO = getById(noteId);

        // 笔记不存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许更新笔记
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 话题
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectById(topicId).getName();

            // 判断提交的话题是否存在
            if (StringUtils.isBlank(topicName)) throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
        }

        // 更新笔记元数据表 t_note
        String content = updateNoteReqVO.getContent();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isContentEmpty(StringUtils.isBlank(content))
                .imgUris(imgUris)
                .title(updateNoteReqVO.getTitle())
                .topicId(updateNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .build();

        updateById(noteDO);

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 删除本地缓存
        // LOCAL_CACHE.invalidate(noteId);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功");

        // 笔记内容更新
        // 查询笔记内容对应的 UUID
        NoteDO noteDO1 = getById(noteId);
        String contentUUID = noteDO1.getContentUuid();

        // 笔记内容更新是否成功
        boolean isUpdateContentSuccess = false;
        if (StringUtils.isBlank(content)) {
            // 若笔记内容为空，则删除 K-V 存储
            isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUUID);
        } else {
            // 调用 K-V 服务更新短文本
            isUpdateContentSuccess = keyValueRpcService.saveNoteContent(contentUUID, content);
        }

        // 若更新失败，则抛出业务异常，回滚事务
        if (!isUpdateContentSuccess) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        return Response.success();
    }

    /**
     * 删除笔记本地缓存
     * @param noteId
     */
    @Override
    public void deleteNoteLocalCache(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
    }

    /**
     * 删除笔记
     * @param deleteNoteReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        // 笔记 ID
        Long noteId = deleteNoteReqVO.getId();

        NoteDO selectNoteDO = getById(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许删除笔记
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 逻辑删除
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .status(NoteStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        boolean update_result = updateById(noteDO);

        if (!update_result) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 删除缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("===> MQ: 删除本地缓存通知发送成功");

        return Response.success();
    }

    /**
     * 笔记设为仅自己可见
     * @param updateNoteVisibleOnlyMeReqVO
     * @return
     */
    @Override
    public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        // 笔记 ID
        Long noteId = updateNoteVisibleOnlyMeReqVO.getId();

        NoteDO selectNoteDO = getById(noteId);
        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许设为仅自己可见
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 构建更新 DO 实体类
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .visible(NoteVisibleEnum.PRIVATE.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        QueryWrapper<NoteDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", noteId);
        wrapper.eq("status", NoteStatusEnum.NORMAL.getCode());

        boolean updated = update(noteDO, wrapper);
        if (!updated) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_SET_VISIBLE_ONLY_ME);
        }

        // 删除 Redis 缓存
        String noteDetailKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailKey);

        // 同步发送 MQ 广播删除所有实例中的本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("===> MQ: 删除本地缓存通知发送成功");

        return Response.success();
    }

    /**
     * 笔记置顶 / 取消置顶
     * @param topNoteReqVO
     * @return
     */
    @Override
    public Response<?> topNote(TopNoteReqVO topNoteReqVO) {
        // 笔记 ID
        Long noteId = topNoteReqVO.getId();
        // 是否置顶
        Boolean isTop = topNoteReqVO.getIsTop();

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();

        // 构建置顶/取消置顶 DO 实体类
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isTop(isTop)
                .updateTime(LocalDateTime.now())
                .creatorId(currUserId) // 仅笔记所有者可置顶/取消置顶该笔记
                .build();

        QueryWrapper<NoteDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", noteId);
        wrapper.eq("creatorId", currUserId);

        boolean updated = update(noteDO, wrapper);

        if (!updated) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("===> MQ: 删除本地缓存通知发送成功");

        return Response.success();
    }

    /**
     * 点赞笔记
     * @param likeNoteReqVO
     * @return
     */
    @Override
    public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {

        // 笔记 ID
        Long noteId = likeNoteReqVO.getId();

        // 1. 校验被点赞的笔记是否存在
        Long creatorId = checkNoteIsExistsAndGetCreatorId(noteId);

        // 2. 判断目标笔记，是否已经点赞过
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器 Key
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);

        // 用户点赞列表 ZSet Key
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua")));
        script.setResultType(Long.class);

        // 执行脚本拿到返回值
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);

        NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);

        switch (noteLikeLuaResultEnum) {
            // Redis 中布隆过滤器不存在
            case NOT_EXISTS -> {
                // 从数据库中校验笔记是否被点赞，并异步初始化布隆过滤器设置，设置过期时间
                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

                // 保底 1 天 + 随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                // 目标笔记已经被点赞
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() ->
                            batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey));
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }

                // 若目标笔记未被点赞，查询该用户是否点赞了其他笔记，有则同步初始化布隆过滤器
                batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);

                // 若数据库中也没有点赞记录，说明该用户还未点赞过任何笔记
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_like_and_expire.lua")));
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId, expireSeconds);
            }
            // 目标笔记已经被点赞 (可能存在误判，需进一步确认)
            case NOTE_LIKED -> {
                // 校验 ZSet 列表中是否包含被点赞的笔记 ID
                Double score = redisTemplate.opsForZSet().score(userNoteLikeZSetKey, noteId);

                if (Objects.nonNull(score)) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
                // 若 score 为空，则表示 ZSet 点赞列表中不存在，查库校验
                int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);

                if (count > 0) {
                    // 数据库中有点赞记录，但 Redis 中 ZSet 不存在，需要重新异步初始化 ZSet
                    asyncInitUserNoteLikesZSet(userId, userNoteLikeZSetKey);
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
            }
        }

        // 3. 更新用户 ZSET 列表
        LocalDateTime now = LocalDateTime.now();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_like_check_and_update_zset.lua")));
        script.setResultType(Long.class);

        result = redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));

        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, NoteLikeLuaResultEnum.NOT_EXISTS.getCode())) {
            // 查询当前用户最新点赞的 100 篇笔记
            List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);

            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                // 保底 1 天 + 随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 构建 luaArgs
                Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                script2.setResultType(Long.class);

                redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);
                // 再次调用 note_like_check_and_update_zset 脚本，将点赞的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            }
        }

        // 4. 发送 MQ 消息，将点赞数据落库
        // 构建消息体 DTO
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeUnlikeNoteTypeEnum.LIKE.getCode())
                .createTime(now)
                .noteCreatorId(creatorId)
                .build();

        // 构建消息对象，将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(likeUnlikeNoteMqDTO)).build();

        // 通过冒号连接，可以让 MQ 发送给 Topic 时，携带 Tag 信息
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;

        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记点赞】MQ 消息发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【笔记点赞】MQ 消息发送异常：", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO) {
        Long noteId = unlikeNoteReqVO.getId();

        Long creatorId = checkNoteIsExistsAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();

        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_note_unlike_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);

        NoteUnlikeLuaResultEnum noteUnlikeLuaResultEnum = NoteUnlikeLuaResultEnum.valueOf(result);

        switch (noteUnlikeLuaResultEnum) {
            // 布隆过滤器不存在
            case NOT_EXISTS -> {
                // 异步初始化布隆过滤器
                threadPoolTaskExecutor.submit(() -> {
                   long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                   batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);
                });
                int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);
                // 未点赞无法进行取消点赞操作
                if (count == 0) throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
            }
            // 布隆过滤器校验笔记未被点赞过（不存在误判）
            case NOTE_NOT_LIKED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
        }

        // 已点赞 => 删除 ZSet 中已点赞的笔记 ID
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
        redisTemplate.opsForZSet().remove(userNoteLikeZSetKey, noteId);

        // 发送消息到 MQ，数据更新落库
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeUnlikeNoteTypeEnum.UNLIKE.getCode())
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(likeUnlikeNoteMqDTO)).build();

        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消点赞】 MQ 消息发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【笔记取消点赞】 MQ 消息发送异常：", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
        Long noteId = collectNoteReqVO.getId();

        Long creatorId = checkNoteIsExistsAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();
        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);

        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_collect_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId);
        NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(result);

        switch (noteCollectLuaResultEnum) {
            case NOT_EXISTS -> { // 布隆过滤器不存
                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 已经被收藏
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() -> {
                        batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                    });
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
                // 若目标笔记未被收藏，查询当前用户是否收藏了其他笔记，有则同步初始化布隆过滤器
                batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                // 添加当前收藏笔记 ID 到布隆过滤器中
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_collect_and_expire.lua")));
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId, expireSeconds);
            }
            case NOTE_COLLECTED -> { // 笔记已经被收藏（可能误判，需进一步确认）
                // 校验 ZSet 列表中是否包含被收藏的笔记 ID
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);

                if (Objects.nonNull(score)) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }

                int count = noteCollectionDOMapper.selectNoteIsCollected(userId, noteId);
                if (count > 0) {
                    // 数据库里有收藏记录，但是 Redis 中没有（可能是过期）的话，就需要异步重建 ZSet 缓存
                    asyncInitUserNoteCollectsZSet(userId, userNoteCollectZSetKey);

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
            }
        }
        // 更新用户 ZSet 收藏列表
        LocalDateTime now = LocalDateTime.now();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_collect_check_and_update_zset.lua")));
        script.setResultType(Long.class);

        result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));

        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, NoteCollectLuaResultEnum.NOT_EXISTS.getCode())) {
            // 查询当前用户最新收藏的 300 篇笔记
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 100);
            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset-and_expire.lua")));
                script.setResultType(Long.class);
                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);

                // 再次调用 note_collect_check_and_update_zset 脚本，把当前收藏的笔记加到 ZSet 中
                redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            }
        }

        // 发送 MQ，将收藏数据落库
        // 构建消息体 DTO
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder().
                userId(userId)
                .noteId(noteId)
                .type(CollectUnCollectNoteTypeEnum.COLLECT.getCode())
                .createTime(now)
                .noteCreatorId(creatorId)
                .build();

        // 构建消息对象，将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(collectUnCollectNoteMqDTO)).build();

        String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_COLLECT;

        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记收藏】 MQ 消息发送成功, sendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【笔记收藏】 MQ 消息发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO) {
        Long noteId = unCollectNoteReqVO.getId();

        Long creatorId = checkNoteIsExistsAndGetCreatorId(noteId);

        Long userId = LoginUserContextHolder.getUserId();

        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_uncollect_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId);

        NoteUnCollectLuaResultEnum noteUnCollectLuaResultEnum = NoteUnCollectLuaResultEnum.valueOf(result);

        switch (noteUnCollectLuaResultEnum) {
            case NOT_EXIST -> {
                threadPoolTaskExecutor.submit(() -> {
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                });

                int count = noteCollectionDOMapper.selectNoteIsCollected(userId, noteId);

                if (count == 0) throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
            }
            case NOTE_NOT_COLLECTED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
        }

        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        redisTemplate.opsForZSet().remove(userNoteCollectZSetKey, noteId);

        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(CollectUnCollectNoteTypeEnum.UN_COLLECT.getCode())
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(collectUnCollectNoteMqDTO)).build();

        String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_UN_COLLECT;

        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消收藏】 MQ 消息发送成功, sendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【笔记取消收藏】 MQ 消息发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 初始化笔记收藏布隆过滤器
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteCollectListKey
     */
    private void batchAddNoteCollect2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteCollectListKey) {
        try {
            // 全量同步用户收藏的笔记
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);

            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua")));
                script.setResultType(Long.class);

                List<Object> luaArgs = Lists.newArrayList();
                noteCollectionDOS.forEach(noteCollectionDO -> luaArgs.add(noteCollectionDO.getNoteId())); // 将每个收藏的笔记 ID 传入
                luaArgs.add(expireSeconds); // 最后一个参数是过期时间
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), luaArgs);
            }
        } catch (Exception e) {
            log.error("## 异步初始化【笔记收藏】布隆过滤器异常: ", e);
        }
    }

    /**
     * 异步初始化用户收藏笔记 ZSet
     * @param userId
     * @param userNoteCollectZSetKey
     */
    private void asyncInitUserNoteCollectsZSet(Long userId, String userNoteCollectZSetKey) {
        threadPoolTaskExecutor.submit(() -> {
            // 判断用户笔记收藏 ZSet 是否存在
            boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(userNoteCollectZSetKey));

            // 不存在则重新初始化
            if (!hasKey) {
                // 查询当前用户最新收藏的 300 篇笔记
                List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 100);
                if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);
                    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                    script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset-and_expire.lua")));
                    script.setResultType(Long.class);

                    redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), luaArgs);
                }
            }
        });
    }

    /**
     * 构建笔记收藏 ZSet Lua脚本参数
     * @param noteCollectionDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildNoteCollectZSetLuaArgs(List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
        int argsLength = noteCollectionDOS.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime()); // score
            luaArgs[i + 1] = noteCollectionDO.getNoteId(); // value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    /**
     * 异步初始化用户点赞笔记 ZSet
     * @param userId
     * @param userNoteLikeZSetKey
     */
    private void asyncInitUserNoteLikesZSet(Long userId, String userNoteLikeZSetKey) {
        threadPoolTaskExecutor.submit(() -> {
            // 判断用户笔记点赞 ZSet 是否存在
            boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(userNoteLikeZSetKey));

            // 不存在则重新初始化
            if (!hasKey) {
                // 查询当前用户最新点赞的 100 篇笔记
                List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);

                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    // 保底 1 天 + 随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    // 构建 luaArgs
                    Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);
                }
            }
        });
    }

    /**
     * 构建 Lua 脚本参数
     * @param noteLikeDOS
     * @param expireSeconds
     * @return
     */
    private Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> noteLikeDOS, long expireSeconds) {
        int argsLength = noteLikeDOS.size() * 2 + 1; // 每个笔记点赞关系有 2 个参数（score, value），最后加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteLikeDO noteLikeDO : noteLikeDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime()); // score: 点赞时间
            luaArgs[i + 1] = noteLikeDO.getNoteId(); // value: 笔记 ID
            i += 2;
        }
        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 初始化布隆过滤器
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteLikeListKey
     */
    private void batchAddNoteLike2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
       try {
           // 异步全量同步一下，并设置过期时间
           List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);

           if (CollUtil.isNotEmpty(noteLikeDOS)) {
               DefaultRedisScript<Long> script = new DefaultRedisScript<>();
               script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
               script.setResultType(Long.class);

               List<Object> luaArgs = Lists.newArrayList();
               noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记 ID 传入
               luaArgs.add(expireSeconds); // 最后一个参数是过期时间(s)
               redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
           }
       } catch (Exception e) {
           log.error("## 异步初始化布隆过滤器异常：", e);
       }
    }

    /**
     * 校验笔记是否存在，若存在，则返回笔记的发布者 ID
     * @param noteId
     */
    private Long checkNoteIsExistsAndGetCreatorId(Long noteId) {
        // 先从本地缓存校验
        String findNoteDetailRespVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        // 解析 Json 字符串为 VO 对象
        FindNoteDetailRespVO findNoteDetailRespVO = JsonUtil.parseObject(findNoteDetailRespVOStrLocalCache, FindNoteDetailRespVO.class);

        // 若本地缓存没有
        if (Objects.isNull(findNoteDetailRespVO)) {
            // 再从 Redis 中校验
            String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);

            String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

            // 解析 Json 字符串 为 VO 对象
            findNoteDetailRespVO = JsonUtil.parseObject(noteDetailJson, FindNoteDetailRespVO.class);

            // 都不存在，查库校验
            if (Objects.isNull(findNoteDetailRespVO)) {
                Long creatorId = noteDOMapper.selectCreatorIdByNoteId(noteId);

                QueryWrapper<NoteDO> wrapper = new QueryWrapper<>();
                wrapper.eq("id", noteId);
                wrapper.eq("status", NoteStatusEnum.NORMAL.getCode());
                long count = count(wrapper);
                // 若数据库中也不存在，抛出业务异常
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
                }

                // 若数据库中存在，异步更新缓存
                threadPoolTaskExecutor.submit(() -> {
                    FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder().id(noteId).build();
                    findNoteDetail(findNoteDetailReqVO); // 查询笔记（自动写入缓存）
                });
                return creatorId;
            }
        }
        return findNoteDetailRespVO.getCreatorId();
    }

    /**
     * 校验笔记的可见性
     * @param visible
     * @param currUserId
     * @param creatorId
     */
    private void checkNoteVisible(Integer visible, Long currUserId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
            && !Objects.equals(currUserId, creatorId)) { // 仅自己可见，且访问用户不是发布者
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

    /**
     * 校验笔记的可见性（针对 VO 实体类）
     * @param userId
     * @param findNoteDetailRespVO
     */
    private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRespVO findNoteDetailRespVO) {
        if (Objects.nonNull(findNoteDetailRespVO)) {
            Integer visible = findNoteDetailRespVO.getVisible();
            checkNoteVisible(visible, userId, findNoteDetailRespVO.getCreatorId());
        }
    }

}




