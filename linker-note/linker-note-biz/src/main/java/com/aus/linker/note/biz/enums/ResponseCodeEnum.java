package com.aus.linker.note.biz.enums;

import com.aus.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("NOTE-50000", "服务器繁忙，请稍后再试"),
    PARAM_NOT_VALID("NOTE-40000", "参数错误"),

    // ----------- 业务异常状态码 -----------
    NOTE_TYPE_ERROR("NOTE-40001", "未知的笔记类型"),
    NOTE_PUBLISH_FAIL("NOTE-50001", "笔记发布失败"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
