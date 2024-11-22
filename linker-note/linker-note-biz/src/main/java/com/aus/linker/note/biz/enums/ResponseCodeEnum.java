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
    NOTE_UPDATE_FAIL("NOTE-50002", "笔记更新失败"),
    NOTE_CANT_SET_VISIBLE_ONLY_ME("NOTE-50003", "该笔记无法设置为仅自己可见"),
    NOTE_NOT_FOUND("NOTE-40400", "笔记不存在"),
    TOPIC_NOT_FOUND("NOTE-40401", "话题不存在"),
    NOTE_PRIVATE("NOTE-40100", "作者已将笔记设为仅自己可见"),
    NOTE_CANT_OPERATE("NOTE-40101", "您无法操作该笔记"),
    NOTE_ALREADY_LIKED("NOTE-40300", "您已经点赞过该笔记"),
    NOTE_NOT_LIKED("NOTE-40301", "您未点赞过该笔记，无法取消点赞"),
    NOTE_ALREADY_COLLECTED("NOTE-40302", "您已经收藏过该笔记"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
