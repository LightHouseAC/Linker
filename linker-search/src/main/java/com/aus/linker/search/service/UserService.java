package com.aus.linker.search.service;

import com.aus.framework.common.response.PageResponse;
import com.aus.linker.search.model.vo.SearchUserReqVO;
import com.aus.linker.search.model.vo.SearchUserRespVO;

public interface UserService {

    /**
     * 搜索用户
     * @param searchUserReqVO
     * @return
     */
    PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO searchUserReqVO);

}
