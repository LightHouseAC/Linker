package com.aus.linker.note.biz.domain.mapper;

import com.aus.linker.note.biz.domain.dataobject.NoteCollectionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author lance.yang
* @description 针对表【t_note_collection(笔记收藏表)】的数据库操作Mapper
* @createDate 2024-11-18 14:52:46
* @Entity com.aus.linker.note.biz.domain.dataobject.NoteCollectionDO
*/
public interface NoteCollectionDOMapper extends BaseMapper<NoteCollectionDO> {

    /**
     * 查询笔记是否被收藏
     * @param userId
     * @param noteId
     * @return
     */
    int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteCollectionDO> selectByUserId(@Param("userId") Long userId);

}




