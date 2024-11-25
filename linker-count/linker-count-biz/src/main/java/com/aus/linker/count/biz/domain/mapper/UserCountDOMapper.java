package com.aus.linker.count.biz.domain.mapper;

import com.aus.linker.count.biz.domain.dataobject.UserCountDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author lance.yang
* @description 针对表【t_user_count(用户计数表)】的数据库操作Mapper
* @createDate 2024-11-18 14:46:59
* @Entity com.aus.linker.count.biz.domain.dataobject.UserCountDO
*/
public interface UserCountDOMapper extends BaseMapper<UserCountDO> {

    /**
     * 添加或更新粉丝总数
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateFansTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 添加或更新关注总数
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateFollowingTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 添加记录或更新笔记点赞数
     * @param count
     * @param userId
     */
    int insertOrUpdateLikeTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 添加记录或更新笔记收藏数
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateCollectTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

}




