package com.aus.linker.auth.enums;

import com.aus.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ------ 通用异常状态码 ------
    SYSTEM_ERROR("AUTH-50000", "系统故障，紧急修复中"),
    PARAM_NOT_VALID("AUTH-40000", "请求参数错误"),

    // ------ 业务异常状态码 ------
    VERIFICATION_CODE_SEND_FREQUENTLY("AUTH-40300", "验证码请求过频繁，请3分钟后重试"),
    VERIFICATION_CODE_WRONG("AUTH-40100", "验证码错误"),
    LOGIN_TYPE_ERROR("AUTH-40001", "登录类型错误"),
    USER_NOT_FOUND("AUTH-40400", "该用户不存在"),
    PHONE_OR_PASSWORD_ERROR("AUTH-40101", "手机号或密码错误"),
    LOGIN_FAIL("AUTH-40102", "登录失败")
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;


}