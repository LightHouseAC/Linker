package com.aus.linker.note.biz.domain.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.note.biz.constant.RedisKeyConstants;
import com.aus.linker.note.biz.domain.dataobject.NoteDO;
import com.aus.linker.note.biz.domain.mapper.NoteDOMapper;
import com.aus.linker.note.biz.domain.mapper.TopicDOMapper;
import com.aus.linker.note.biz.domain.service.NoteService;
import com.aus.linker.note.biz.enums.NoteStatusEnum;
import com.aus.linker.note.biz.enums.NoteTypeEnum;
import com.aus.linker.note.biz.enums.NoteVisibleEnum;
import com.aus.linker.note.biz.enums.ResponseCodeEnum;
import com.aus.linker.note.biz.model.vo.FindNoteDetailReqVO;
import com.aus.linker.note.biz.model.vo.FindNoteDetailRespVO;
import com.aus.linker.note.biz.model.vo.PublishNoteReqVO;
import com.aus.linker.note.biz.model.vo.UpdateNoteReqVO;
import com.aus.linker.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.aus.linker.note.biz.rpc.KeyValueRpcService;
import com.aus.linker.note.biz.rpc.UserRpcService;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
                redisTemplate.opsForValue().set(noteDetailRedisKey, "null", expireSeconds);
            });
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 可见性校验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());

        // RPC: 调用用户服务 获取 发布者详情
        Long creatorId = noteDO.getCreatorId();
        FindUserByIdRespDTO findUserByIdRespDTO = userRpcService.findById(creatorId);

        // RPC: 调用 KV 存储服务获取笔记内容
        String content = null;
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            content = keyValueRpcService.findNoteContent(noteDO.getContentUuid());
        }

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
        FindNoteDetailRespVO findNoteDetailRespVO = FindNoteDetailRespVO.builder()
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

        // 异步线程将笔记详情存入 Redis
        threadPoolTaskExecutor.execute(() -> {
            String noteDetailJson1 = JsonUtil.toJsonString(findNoteDetailRespVO);
            // 过期时间（保底1天 + 随机秒数，防止雪崩）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(noteDetailRedisKey, noteDetailJson1, expireSeconds);
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
        LOCAL_CACHE.invalidate(noteId);

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




