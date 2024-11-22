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

    /**
     * 查询用户所有收藏的笔记
     * @param userId
     * @return
     */
    List<NoteCollectionDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询笔记是否已经被收藏
     * @param userId
     * @param noteId
     * @return
     */
    int selectNoteIsCollected(@Param("userId") Long userId, @Param("noteId") Long noteId);

    /**
     * 查询用户最近收藏的笔记
     * @param userId
     * @param limit
     * @return
     */
    List<NoteCollectionDO> selectCollectedByUserIdAndLimit(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 新增笔记收藏记录，若已存在，则更新笔记收藏记录
     * @param noteCollectionDO
     * @return
     */
    int insertOrUpdate(NoteCollectionDO noteCollectionDO);

    /**
     * 取消点赞
     * @param noteCollectionDO
     * @return
     */
    int update2UnCollectByUserIdAndNoteId(NoteCollectionDO noteCollectionDO);

}




