package com.aus.linker.auth.domain.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.auth.domain.dataobject.UserDO;
import com.aus.linker.auth.model.vo.user.UserLoginReqVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service
* @createDate 2024-08-22 16:56:14
*/
public interface UserService extends IService<UserDO> {

    /**
     * 登录与注册
     * @param userLoginReqVO
     * @return
     */
    Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout();

}
