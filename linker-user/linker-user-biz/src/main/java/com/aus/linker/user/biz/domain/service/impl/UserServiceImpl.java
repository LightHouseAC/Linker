package com.aus.linker.user.biz.domain.service.impl;

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
import com.aus.linker.user.dto.req.FindUserByPhoneReqDTO;
import com.aus.linker.user.dto.req.RegisterUserReqDTO;
import com.aus.linker.user.dto.req.UpdateUserPasswordReqDTO;
import com.aus.linker.user.dto.resp.FindUserByPhoneRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private RedisTemplate<String, String> redisTemplate;

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




