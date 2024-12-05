package com.aus.linker.data.align.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUnfollowMqDTO {

    private Long userId;

    private Long targetUserId;

    /**
     * 0: 取关，1: 关注
     */
    private Integer type;

}
