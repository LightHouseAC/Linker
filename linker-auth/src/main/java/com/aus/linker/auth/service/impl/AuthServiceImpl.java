package com.aus.linker.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.linker.auth.constant.RedisKeyConstants;
import com.aus.linker.auth.enums.LoginTypeEnum;
import com.aus.linker.auth.enums.ResponseCodeEnum;
import com.aus.linker.auth.model.vo.user.UpdatePasswordReqVO;
import com.aus.linker.auth.model.vo.user.UserLoginReqVO;
import com.aus.linker.auth.rpc.UserRpcService;
import com.aus.linker.auth.service.AuthService;
import com.aus.linker.user.dto.resp.FindUserByPhoneRespDTO;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service实现
* @createDate 2024-08-22 16:56:14
*/
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserRpcService userRpcService;

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

                // RPC: 调用用户服务，注册用户
                Long userIdTmp = userRpcService.registerUser(phone);

                // 若调用用户服务，返回的用户 ID 为空，则提示登录失败
                if (Objects.isNull(userIdTmp)) {
                    throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                }
                userId = userIdTmp;
                break;
            case PASSWORD:  // 密码登录
                String password = userLoginReqVO.getPassword();

                // RPC: 调用用户服务，通过手机号查询用户
                FindUserByPhoneRespDTO findUserByPhoneRespDTO = userRpcService.findUserByPhone(phone);

                // 判断该手机号是否注册
                if (Objects.isNull(findUserByPhoneRespDTO)) {
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                // 拿到密文密码
                String encodePassword = findUserByPhoneRespDTO.getPassword();

                // 匹配密码是否一致
                boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

                if (!isPasswordCorrect) {
                    throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }

                userId = findUserByPhoneRespDTO.getId();
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

        // RPC: 调用用户服务，根棍密码
        userRpcService.updatePassword(encodePassword);

        return Response.success();
    }

}