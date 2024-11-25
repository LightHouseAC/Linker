package com.aus.linker.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NoteOperateEnum {

    // 笔记发布
    PUBLISH(1),
    // 笔记删除
    DELETE(0),
    ;

    private final Integer code;

    public static NoteOperateEnum valueOf(Integer code) {
        for (NoteOperateEnum noteOperateEnum : NoteOperateEnum.values()) {
            if (Objects.equals(code, noteOperateEnum.getCode())) {
                return noteOperateEnum;
            }
        }
        return null;
    }

}
