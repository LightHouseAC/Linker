package com.aus.linker.count.biz.domain.mapper;

import com.aus.linker.count.biz.domain.dataobject.NoteCountDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author lance.yang
* @description 针对表【t_note_count(笔记计数表)】的数据库操作Mapper
* @createDate 2024-11-18 14:45:35
* @Entity com.aus.linker.count.biz.domain.dataobject.NoteCountDO
*/
public interface NoteCountDOMapper extends BaseMapper<NoteCountDO> {

    /**
     * 添加笔记计数记录或更新笔记点赞数
     * @param count
     * @param noteId
     * @return
     */
    int insertOrUpdateLikeTotalByNoteId(@Param("count") Integer count, @Param("noteId") Long noteId);

}




