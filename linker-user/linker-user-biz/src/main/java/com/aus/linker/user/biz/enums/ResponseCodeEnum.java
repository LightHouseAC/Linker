package com.aus.linker.user.biz.enums;

import com.aus.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ---------- 通用异常状态码 -----------
    SYSTEM_ERROR("USER-50000", "系统故障，紧急修复中"),
    PARAM_NOT_VALID("USER-40000", "请求参数错误"),

    // ---------- 业务异常状态码 -----------
    NICK_NAME_VALID_FAIL("USER-40001", "昵称请设置2-24个字符，不能使用@《/等特殊字符"),
    LINKER_ID_VALID_FAIL("USER-40002", "Linker 号请设置6-15个字符，仅可使用英文（必须）、数字、下划线"),
    SEX_VALID_FAIL("USER-40003", "性别错误"),
    INTRODUCTION_VALID_FAIL("USER-40004", "个人简介请设置1-100个字符"),
    UPLOAD_AVATAR_FAIL("USER-50001", "头像上传失败"),
    UPLOAD_BACKGROUND_IMG_FAIL("USER-50002", "背景图上传失败"),
    USER_NOT_FOUND("USER-40400", "该用户不存在")
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
