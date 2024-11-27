package com.aus.linker.data.align.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectUnCollectMqDTO {

    private Long userId;

    private Long noteId;

    /**
     * 1 - 收藏， 0 - 取消收藏
     */
    private Integer type;

    private LocalDateTime createTime;

    private Long noteCreatorId;

}
