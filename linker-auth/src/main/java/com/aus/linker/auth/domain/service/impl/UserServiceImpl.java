package com.aus.linker.auth.domain.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.auth.constant.RedisKeyConstants;
import com.aus.linker.auth.constant.RoleConstants;
import com.aus.linker.auth.domain.dataobject.RoleDO;
import com.aus.linker.auth.domain.dataobject.UserDO;
import com.aus.linker.auth.domain.dataobject.UserRoleDO;
import com.aus.linker.auth.domain.mapper.RoleDOMapper;
import com.aus.linker.auth.domain.mapper.UserDOMapper;
import com.aus.linker.auth.domain.mapper.UserRoleDOMapper;
import com.aus.linker.auth.domain.service.UserService;
import com.aus.linker.auth.enums.DeletedEnum;
import com.aus.linker.auth.enums.LoginTypeEnum;
import com.aus.linker.auth.enums.ResponseCodeEnum;
import com.aus.linker.auth.enums.StatusEnum;
import com.aus.linker.auth.model.vo.user.UpdatePasswordReqVO;
import com.aus.linker.auth.model.vo.user.UserLoginReqVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service实现
* @createDate 2024-08-22 16:56:14
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserDOMapper, UserDO>
    implements UserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserRoleDOMapper userRoleDOMapper;

    /**
     * 编程式事务模板
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RoleDOMapper roleDOMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 登录与注册
     * @param userLoginReqVO
     * @return
     */
    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);
        Long userId = null;
        // 判断登陆类型
        switch (loginTypeEnum) {
            case VERIFICATION_CODE: // 手机号+验证码登录
                String verificationCode = userLoginReqVO.getCode();
                // 检验入参验证码是否为空
//                if (StringUtils.isBlank(verificationCode)) {
//                    return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "验证码不能为空");
//                }
                Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空");

                // 构建验证码Redis key
                String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                // 查询存储在Redis中的验证码
                String sentCode = (String) redisTemplate.opsForValue().get(key);

                // 判断用户提交的验证码与Redis中存的是否一致
                if (!StringUtils.equals(verificationCode, sentCode)) {
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_WRONG);
                }

                // 通过手机号查询用户
                QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("phone", phone);
                UserDO userDO = this.getOne(queryWrapper);
                log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtil.toJsonString(userDO));

                // 如果还未注册
                if (Objects.isNull(userDO)) {
                    // 若此用户还未注册，系统自动注册用户
                    userId = registerUser(phone);
                } else {
                    // 已注册则获取用户id
                    userId = userDO.getId();
                }
                break;
            case PASSWORD:
                String password = userLoginReqVO.getPassword();
                // 根据手机号查询
                QueryWrapper<UserDO> queryWrapper1 = new QueryWrapper<>();
                queryWrapper1.eq("phone", phone);
                UserDO userDO1 = getOne(queryWrapper1);

                // 判断该手机号是否注册
                if (Objects.isNull(userDO1)) {
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                // 拿到密文密码
                String encodePassword = userDO1.getPassword();

                // 匹配密码是否一致
                boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

                if (!isPasswordCorrect) {
                    throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }

                userId = userDO1.getId();
                break;
            default:
                break;
        }

        // SaToken 登录用户，并返回Token令牌
        StpUtil.login(userId);

        // 获取Token令牌
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // 返回Token令牌
        return Response.success(tokenInfo.tokenValue);

    }

    /**
     * 退出登录
     * @return
     */
    @Override
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();

        log.info("==> 用户退出登录，userId: {}", userId);

        // 退出登录（指定用户 ID）
        StpUtil.logout(userId);

        return Response.success();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        // 新密码
        String newPassword = updatePasswordReqVO.getNewPassword();
        // 密码加密
        String encodePassword = passwordEncoder.encode(newPassword);

        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(encodePassword)
                .updateTime(LocalDateTime.now())
                .build();

        // 更新密码
        updateById(userDO);

        return Response.success();
    }

    /**
     * 系统自动注册用户
     * @param phone
     * @return
     */
//    @Transactional(rollbackFor = Exception.class)
    public Long registerUser(String phone) {
        return transactionTemplate.execute(status -> {
            try {
                // 获取全局自增的Linker ID
                Long linkerId = redisTemplate.opsForValue().increment(RedisKeyConstants.LINKER_ID_GENERATOR_KEY);

                UserDO userDO = UserDO.builder()
                        .phone(phone)
                        .linkerId(String.valueOf(linkerId)) // 自动生成Linker ID
                        .nickname("林克" + linkerId) // 自动生成昵称
                        .status(StatusEnum.Enabled.getValue()) // 状态为启用
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                        .build();

                // 保存进数据库
                this.save(userDO);

                // 获取用户id
                Long userId = userDO.getId();

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