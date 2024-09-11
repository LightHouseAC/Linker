package com.aus.linker.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.linker.auth.constant.RedisKeyConstants;
import com.aus.linker.auth.service.VerificationCodeService;
import com.aus.linker.auth.enums.ResponseCodeEnum;
import com.aus.linker.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.aus.linker.auth.sms.AliyunSmsHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private AliyunSmsHelper aliyunSmsHelper;

    /**
     * 发送短信验证码
     * @param sendVerificationCodeReqVO
     * @return
     */
    @Override
    public Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO){
        // 手机号
        String phone = sendVerificationCodeReqVO.getPhone();
        // 构建短信验证码redis key
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        // 判断是否已发送验证码
        boolean isSent = redisTemplate.hasKey(key);
        if (isSent) {
            // 若之前发送的验证码未过期，则提示发送频繁
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }
        // 生成6位随机数字验证码
        String verificationCode = RandomUtil.randomNumbers(6);

        log.info("==> 手机号: {}, 已生成验证码: 【{}】", phone, verificationCode);

        // 调用第三方短信发送服务
        threadPoolTaskExecutor.submit(() -> {
            String signName = "阿里云短信测试";
            String templateCode = "SMS_154950909";
            String templateParam = String.format("{\"code\":\"%s\"}", verificationCode);
            aliyunSmsHelper.sendMessage(signName, templateCode, phone, templateParam);
        });

        // 存储验证码到redis并设置好过期时间
        redisTemplate.opsForValue().set(key, verificationCode, 3, TimeUnit.MINUTES);

        return Response.success();
    }

}
