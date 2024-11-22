package com.aus.linker.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NoteCollectLuaResultEnum {

    // 布隆过滤器 或 ZSet 不存在
    NOT_EXISTS(-1L),
    // 笔记已收藏过
    NOTE_COLLECTED(1L),
    // 笔记收藏成功
    NOTE_COLLECT_SUCCESS(0L),
    ;

    private final Long code;

    public static NoteCollectLuaResultEnum valueOf(Long code) {
        for (NoteCollectLuaResultEnum noteCollectLuaResultEnum : NoteCollectLuaResultEnum.values()) {
            if (Objects.equals(code, noteCollectLuaResultEnum.getCode())) {
                return noteCollectLuaResultEnum;
            }
        }
        return null;
    }

}
