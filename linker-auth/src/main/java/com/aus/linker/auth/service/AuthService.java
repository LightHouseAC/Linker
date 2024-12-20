package com.aus.linker.auth.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.auth.model.vo.user.UpdatePasswordReqVO;
import com.aus.linker.auth.model.vo.user.UserLoginReqVO;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service
* @createDate 2024-08-22 16:56:14
*/
public interface AuthService {

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

    /**
     * 修改密码
     * @param updatePasswordReqVO
     * @return
     */
    Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);

}
