package com.aus.linker.user.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusEnum {

    // 启用
    Enabled(0),
    // 禁用
    Disabled(1);

    private final Integer value;

}
