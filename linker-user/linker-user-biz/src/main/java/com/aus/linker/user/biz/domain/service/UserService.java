package com.aus.linker.user.biz.domain.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.user.biz.domain.dataobject.UserDO;
import com.aus.linker.user.biz.model.vo.UpdateUserInfoReqVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service
* @createDate 2024-09-07 21:58:57
*/
public interface UserService extends IService<UserDO> {

    /**
     * 更新用户信息
     * @param updateUserInfoReqVO
     * @return
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

}
