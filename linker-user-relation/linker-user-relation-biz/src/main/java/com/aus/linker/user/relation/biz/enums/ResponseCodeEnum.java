package com.aus.linker.user.relation.biz.enums;

import com.aus.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("RELATION-50000", "系统故障，紧急修复中"),
    PARAM_NOT_VALID("RELATION-40000", "请求参数错误"),

    // ----------- 业务异常状态码 -----------
    CANT_FOLLOW_YOURSELF("RELATION-40300", "无法关注自己"),
    FOLLOW_USER_NOT_EXISTS("RELATION-40400", "关注的用户不存在"),
    FOLLOWING_COUNT_LIMIT("RELATION-40301", "您关注的用户已达上限，请先取关部分用户"),
    ALREADY_FOLLOWED("RELATION-40302", "您已经关注了该用户"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
