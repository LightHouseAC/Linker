package com.aus.linker.user.relation.biz.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.user.relation.biz.model.vo.FollowUserReqVO;
import org.springframework.stereotype.Service;

@Service
public interface RelationService {

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    Response<?> follow(FollowUserReqVO followUserReqVO);

}
