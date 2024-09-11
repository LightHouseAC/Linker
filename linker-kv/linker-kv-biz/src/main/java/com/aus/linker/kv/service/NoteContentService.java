package com.aus.linker.kv.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.kv.dto.req.AddNoteContentReqDTO;
import com.aus.linker.kv.dto.req.DeleteNoteContentReqDTO;
import com.aus.linker.kv.dto.req.FindNoteContentReqDTO;
import com.aus.linker.kv.dto.resp.FindNoteContentRespDTO;

public interface NoteContentService {

    /**
     * 添加笔记内容
     * @param addNoteContentReqDTO
     * @return
     */
    Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

    /**
     * 查询笔记内容
     * @param findNoteContentReqDTO
     * @return
     */
    Response<FindNoteContentRespDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

    /**
     * 删除笔记内容
     * @param deleteNoteContentReqDTO
     * @return
     */
    Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO);

}
