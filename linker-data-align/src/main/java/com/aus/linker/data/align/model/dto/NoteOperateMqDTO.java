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
public class NoteOperateMqDTO {

    private Long creatorId;

    private Long noteId;

    /**
     * 操作类型：0 - 笔记删除，1 - 笔记发布
     */
    private Integer type;

}