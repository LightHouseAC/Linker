package com.aus.linker.user.relation.biz.service;

import com.aus.framework.common.response.PageResponse;
import com.aus.framework.common.response.Response;
import com.aus.linker.user.relation.biz.model.vo.FindFollowingListReqVO;
import com.aus.linker.user.relation.biz.model.vo.FindFollowingListRespVO;
import com.aus.linker.user.relation.biz.model.vo.FollowUserReqVO;
import com.aus.linker.user.relation.biz.model.vo.UnfollowUserReqVO;
import org.springframework.stereotype.Service;

@Service
public interface RelationService {

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    Response<?> follow(FollowUserReqVO followUserReqVO);

    /**
     * 取关用户
     * @param unfollowUserReqVO
     * @return
     */
    Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO);

    /**
     * 查询关注列表
     * @param findFollowingListReqVO
     * @return
     */
    PageResponse<FindFollowingListRespVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO);

}
