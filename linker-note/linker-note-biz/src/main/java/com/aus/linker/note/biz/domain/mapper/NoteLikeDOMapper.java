package com.aus.linker.note.biz.domain.mapper;

import com.aus.linker.note.biz.domain.dataobject.NoteLikeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author lance.yang
* @description 针对表【t_note_like(笔记点赞表)】的数据库操作Mapper
* @createDate 2024-11-18 14:53:05
* @Entity com.aus.linker.note.biz.domain.dataobject.NoteLikeDO
*/
public interface NoteLikeDOMapper extends BaseMapper<NoteLikeDO> {

    int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteLikeDO> selectByUserId(@Param("userId") Long userId);

}




