package com.aus.linker.count.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum LikeUnlikeTypeEnum {

    // 点赞
    LIKE(1),
    // 取消点赞
    UNLIKE(0),
    ;

    private final Integer code;

    public static LikeUnlikeTypeEnum valueOf(Integer code) {
        for (LikeUnlikeTypeEnum likeUnlikeTypeEnum : LikeUnlikeTypeEnum.values()) {
            if (Objects.equals(code, likeUnlikeTypeEnum.getCode())) {
                return likeUnlikeTypeEnum;
            }
        }
        return null;
    }

}
