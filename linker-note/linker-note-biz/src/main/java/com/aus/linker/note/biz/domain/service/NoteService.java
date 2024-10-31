package com.aus.linker.note.biz.domain.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.note.biz.domain.dataobject.NoteDO;
import com.aus.linker.note.biz.model.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author recww
* @description 针对表【t_note(笔记表)】的数据库操作Service
* @createDate 2024-09-24 00:14:08
*/
public interface NoteService extends IService<NoteDO> {

    /**
     * 笔记发布
     * @param publishNoteReqVO
     * @return
     */
    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

    /**
     * 笔记详情
     * @param findNoteDetailReqVO
     * @return
     */
    Response<FindNoteDetailRespVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

    /**
     * 笔记更新
     * @param updateNoteReqVO
     * @return
     */
    Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);

    /**
     * 删除笔记本地缓存
     * @param noteId
     */
    void deleteNoteLocalCache(Long noteId);

    /**
     * 删除笔记
     * @param deleteNoteReqVO
     * @return
     */
    Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

}
