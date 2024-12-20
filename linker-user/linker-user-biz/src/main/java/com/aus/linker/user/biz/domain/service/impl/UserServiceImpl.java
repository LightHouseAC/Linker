package com.aus.linker.user.biz.domain.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.framework.common.utils.ParamUtils;
import com.aus.linker.oss.api.FileFeignApi;
import com.aus.linker.user.biz.constant.RedisKeyConstants;
import com.aus.linker.user.biz.constant.RoleConstants;
import com.aus.linker.user.biz.domain.dataobject.RoleDO;
import com.aus.linker.user.biz.domain.dataobject.UserDO;
import com.aus.linker.user.biz.domain.dataobject.UserRoleDO;
import com.aus.linker.user.biz.domain.mapper.RoleDOMapper;
import com.aus.linker.user.biz.domain.mapper.UserDOMapper;
import com.aus.linker.user.biz.domain.mapper.UserRoleDOMapper;
import com.aus.linker.user.biz.domain.service.UserService;
import com.aus.linker.user.biz.enums.DeletedEnum;
import com.aus.linker.user.biz.enums.ResponseCodeEnum;
import com.aus.linker.user.biz.enums.SexEnum;
import com.aus.linker.user.biz.enums.StatusEnum;
import com.aus.linker.user.biz.model.vo.UpdateUserInfoReqVO;
import com.aus.linker.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.aus.linker.user.biz.rpc.OssRpcService;
import com.aus.linker.user.dto.req.*;
import com.aus.linker.user.dto.resp.FindMultiUserByIdsRespDTO;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.aus.linker.user.dto.resp.FindUserByPhoneRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service实现
* @createDate 2024-09-07 21:58:57
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserDOMapper, UserDO>
    implements UserService {

    @Resource
    private FileFeignApi fileFeignApi;

    @Resource
    private OssRpcService ossRpcService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserRoleDOMapper userRoleDOMapper;

    @Resource
    private RoleDOMapper roleDOMapper;

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    /**
     * 编程式事务模板
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 自定义的异步线程池
     */
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private static final Cache<Long, FindUserByIdRespDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = UserDO.builder().build();
        // 设置当前需要更新的用户 ID
        userDO.setId(LoginUserContextHolder.getUserId());
        // 标识：是否需要更新
        boolean needUpdate = false;

        // 头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();

        if (Objects.nonNull(avatarFile)) {
            String avatar = ossRpcService.uploadFile(avatarFile);
            log.info("==> 调用 oss 服务成功，上传头像，url: {}", avatar);

            // 若上传头像失败，则抛出业务异常
            if (StringUtils.isBlank(avatar)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }

            userDO.setAvatar(avatar);
            needUpdate = true;
        }

        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickName(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL);
            userDO.setNickname(nickname);
            needUpdate = true;
        }

        // Linker 号
        String linkerId = updateUserInfoReqVO.getLinkerId();
        if (StringUtils.isNotBlank(linkerId)) {
            Preconditions.checkArgument(ParamUtils.checkLinkerId(linkerId), ResponseCodeEnum.LINKER_ID_VALID_FAIL);
            userDO.setLinkerId(linkerId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL);
            userDO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL);
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            String backgroundImg = ossRpcService.uploadFile(backgroundImgFile);
            log.info("==> 调用 oss 服务成功，上传背景图，url: {}", backgroundImg);

            // 若上传背景图失败，则抛出业务异常
            if (StringUtils.isBlank(backgroundImg)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }

            userDO.setBackgroundImg(backgroundImg);
            needUpdate = true;
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            updateById(userDO);
        }

        return Response.success();
    }

    /**
     * 用户注册
     * @param registerUserReqDTO
     * @return
     */
    @Override
//    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();

        // 先判断该手机号是否已被注册
        QueryWrapper<UserDO> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("phone", phone);
        UserDO userDO1 = getOne(wrapper1);

        log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtil.toJsonString(userDO1));
        // 若已注册，则直接返回用户 ID
        if (Objects.nonNull(userDO1)) {
            return Response.success(userDO1.getId());
        }

        // 否则注册新用户
        Long userId = registerUser(phone);
        return Response.success(userId);
    }

    @Override
    public Response<FindUserByPhoneRespDTO> findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        String phone = findUserByPhoneReqDTO.getPhoneNumber();

        // 根据手机号查询用户信息
        QueryWrapper<UserDO> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        UserDO userDO = getOne(wrapper);

        // 判空
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建反参
        FindUserByPhoneRespDTO findUserByPhoneRespDTO = FindUserByPhoneRespDTO.builder()
                .id(userDO.getId())
                .password(userDO.getPassword())
                .build();

        return Response.success(findUserByPhoneRespDTO);
    }

    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(updateUserPasswordReqDTO.getEncodePassword()) // 加密后的密码
                .updateTime(LocalDateTime.now())
                .build();
        // 更新密码
        updateById(userDO);
        return Response.success();
    }

    /**
     * 根据用户 ID 查询用户信息
     * @param findUserByIdReqDTO
     * @return
     */
    @Override
    public Response<FindUserByIdRespDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long userID = findUserByIdReqDTO.getId();

        // 先从本地缓存中查询
        FindUserByIdRespDTO findUserByIdRespDTOLocalCache = LOCAL_CACHE.getIfPresent(userID);
        if (Objects.nonNull(findUserByIdRespDTOLocalCache)) {
            log.info("==> 命中了本地缓存：{}", findUserByIdRespDTOLocalCache);
            return Response.success(findUserByIdRespDTOLocalCache);
        }

        // 用户缓存 KEY
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userID);

        // 先从 Redis 缓存中查询
        String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoRedisKey);

        // 若 Redis 缓存中存在该用户信息
        if (StringUtils.isNotBlank(userInfoRedisValue)) {
            // 将存储的 Json 字符串转换成对象并返回
            FindUserByIdRespDTO findUserByIdRespDTO = JsonUtil.parseObject(userInfoRedisValue, FindUserByIdRespDTO.class);
            // 异步线程将用户信息存入本地缓存
            threadPoolTaskExecutor.submit(() -> {
                if (Objects.nonNull(findUserByIdRespDTO)) {
                    // 写入本地缓存
                    LOCAL_CACHE.put(userID, findUserByIdRespDTO);
                }
            });
            return Response.success(findUserByIdRespDTO);
        }

        // 否则，从数据库中查询
        // 根据用户 ID 查询用户信息
        UserDO userDO = getById(userID);

        // 判空
        if (Objects.isNull(userDO)) {

            // 防止缓存穿透，将空数据写入 Redis （给一个短的过期时间）
            // 保底 1 分钟 + 随机秒数
            long expireSeconds = 60 + RandomUtil.randomInt(60);
            redisTemplate.opsForValue().set(userInfoRedisKey, "", expireSeconds, TimeUnit.SECONDS);

            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建返参
        FindUserByIdRespDTO findUserByIdRespDTO = FindUserByIdRespDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .build();

        // 异步将用户信息写入 Redis 缓存，提升响应速度
        threadPoolTaskExecutor.submit(() -> {
            // 过期时间，大N + 小N法，保底1天 + 随机秒数，避免大量缓存同时失效
            long expireSeconds = 60*60+24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue()
                    .set(userInfoRedisKey, JsonUtil.toJsonString(findUserByIdRespDTO), expireSeconds, TimeUnit.SECONDS);
        });

        return Response.success(findUserByIdRespDTO);
    }

    @Override
    public Response<List<FindMultiUserByIdsRespDTO>> findByIds(FindMultiUserByIdsReqDTO findMultiUserByIdsReqDTO) {
        // 需要查询的用户 ID 集合
        List<Long> userIds = findMultiUserByIdsReqDTO.getIds();

        // 构建 Redis Key 集合
        List<String> redisKeys = userIds.stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();

        // 先从 Redis 缓存中查，multiGet 批量查询提升性能
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
        // 如果缓存中不为空
        if (CollUtil.isNotEmpty(redisValues)) {
            // 过滤掉为空的数据
            redisValues = redisValues.stream().filter(Objects::nonNull).toList();
        }

        // 返参
        List<FindMultiUserByIdsRespDTO> findMultiUserByIdsRespDTOS = Lists.newArrayList();

        // 将过滤后的缓存集合，转换为 DTO 返参实体类
        if (CollUtil.isNotEmpty(redisValues)) {
            findMultiUserByIdsRespDTOS = redisValues.stream()
                    .map(value -> JsonUtil.parseObject(String.valueOf(value), FindMultiUserByIdsRespDTO.class))
                    .collect(Collectors.toList());
        }

        // 如果被查询的用户信息，都在 Redis 缓存中，则直接返回
        if (CollUtil.size(userIds) == CollUtil.size(findMultiUserByIdsRespDTOS)) {
            return Response.success(findMultiUserByIdsRespDTOS);
        }

        // 其他情况：1) 缓存里没有用户信息数据 2) 缓存中数据不全，需去数据库中查询
        // 筛选出缓存里没有的用户数据，查询数据库
        List<Long> userIdsNeedQuery = null;

        if (CollUtil.isNotEmpty(findMultiUserByIdsRespDTOS)) {
            // 将 findMultiUsersByIdsRespDTOS 集合转 Map
            Map<Long, FindMultiUserByIdsRespDTO> map = findMultiUserByIdsRespDTOS.stream()
                    .collect(Collectors.toMap(FindMultiUserByIdsRespDTO::getId, p -> p));

            // 筛选出需要查 DB 的用户 ID
            userIdsNeedQuery = userIds.stream()
                    .filter(id -> Objects.isNull(map.get(id)))
                    .toList();
        } else { // 缓存中一条用户信息都没查到，则提交的用户 ID 集合都需要查数据库
            userIdsNeedQuery = userIds;
        }

        // 从数据库中批量查询
        List<UserDO> userDOS = listByIds(userIdsNeedQuery);

        List<FindMultiUserByIdsRespDTO> findMultiUserByIdsRespDTOS1 = null;

        // 若数据库查询的记录不为空
        if (CollUtil.isNotEmpty(userDOS)) {
            // DO 转 DTO
            findMultiUserByIdsRespDTOS1 = userDOS.stream()
                    .map(userDO -> FindMultiUserByIdsRespDTO.builder()
                            .id(userDO.getId())
                            .nickName(userDO.getNickname())
                            .avatar(userDO.getAvatar())
                            .introduction(userDO.getIntroduction())
                            .build())
                    .collect(Collectors.toList());

            // 异步线程将用户信息同步到 Redis 中
            List<FindMultiUserByIdsRespDTO> finalFindMultiUserByIdsRespDTOS = findMultiUserByIdsRespDTOS1;
            threadPoolTaskExecutor.submit(() -> {
                // DTO 集合转 Map
                Map<Long, FindMultiUserByIdsRespDTO> map = finalFindMultiUserByIdsRespDTOS.stream()
                        .collect(Collectors.toMap(FindMultiUserByIdsRespDTO::getId, p -> p));

                // 执行 pipeline 操作
                redisTemplate.executePipelined((RedisCallback<Void>) connection-> {
                    for (UserDO userDO : userDOS) {
                        Long userId = userDO.getId();

                        // 用户信息缓存 Redis Key
                        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);

                        // DTO 转 Json 字符串
                        FindMultiUserByIdsRespDTO findMultiUserByIdsRespDTO = map.get(userId);
                        String value = JsonUtil.toJsonString(findMultiUserByIdsRespDTO);

                        // 过期时间（保底 1 天 + 随机秒数）
                        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                        redisTemplate.opsForValue().set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                    }
                    return null;
                });
            });
        }

        // 合并数据
        if (CollUtil.isNotEmpty(findMultiUserByIdsRespDTOS1)) {
            findMultiUserByIdsRespDTOS.addAll(findMultiUserByIdsRespDTOS1);
        }

        return Response.success(findMultiUserByIdsRespDTOS);

    }

    /**
     * 编程式事务创建用户
     * @param phone
     * @return
     */
    private Long registerUser(String phone) {
        return transactionTemplate.execute(status -> {
            try {
                // 获取全局自增的Linker ID
                // Long linkerId = redisTemplate.opsForValue().increment(RedisKeyConstants.LINKER_ID_GENERATOR_KEY);

                // RPC: 调用分布式 ID 生成服务生成 Linker ID
                String linkerId = distributedIdGeneratorRpcService.getLinkerId();

                // RPC: 调用分布式 ID 生成服务生成 用户 ID
                String userIdStr = distributedIdGeneratorRpcService.getUserId();
                Long userId = Long.valueOf(userIdStr);

                UserDO userDO = UserDO.builder()
                        .id(userId)
                        .phone(phone)
                        .linkerId(linkerId) // 自动生成Linker ID
                        .nickname("林克" + linkerId) // 自动生成昵称
                        .status(StatusEnum.Enabled.getValue()) // 状态为启用
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                        .build();

                // 保存进数据库
                this.save(userDO);

                // 获取用户id
                // Long userId = userDO.getId();

                // 为该用户分配一个默认角色
                UserRoleDO userRoleDO = UserRoleDO.builder()
                        .userId(userId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue())
                        .build();
                userRoleDOMapper.insert(userRoleDO);

                QueryWrapper<RoleDO> wrapper = new QueryWrapper<>();
                wrapper.eq("id", RoleConstants.COMMON_USER_ROLE_ID);
                RoleDO roleDO = roleDOMapper.selectOne(wrapper);

                // 将该用户的角色 ID 存入 Redis 中，指定初始容量为 1， 可以减少扩容时的性能开销
                List<String> roles = new ArrayList<>(1);
                roles.add(roleDO.getRoleKey());

                String userRoleKey = RedisKeyConstants.buildUserRoleKey(userId);
                redisTemplate.opsForValue().set(userRoleKey, JsonUtil.toJsonString(roles));

                return userId;
            } catch (Exception e) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("==> 系统注册用户异常: ", e);
                return null;
            }
        });
    }

}




