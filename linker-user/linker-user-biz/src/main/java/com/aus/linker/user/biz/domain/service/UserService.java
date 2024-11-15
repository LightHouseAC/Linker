package com.aus.linker.user.biz.domain.service;

import com.aus.framework.common.response.Response;
import com.aus.linker.user.biz.domain.dataobject.UserDO;
import com.aus.linker.user.biz.model.vo.UpdateUserInfoReqVO;
import com.aus.linker.user.dto.req.*;
import com.aus.linker.user.dto.resp.FindMultiUserByIdsRespDTO;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.aus.linker.user.dto.resp.FindUserByPhoneRespDTO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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


    /**
     * 用户注册
     * @param registerUserReqDTO
     * @return
     */
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据手机号查询用户信息
     * @param findUserByPhoneReqDTO
     * @return
     */
    Response<FindUserByPhoneRespDTO> findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    /**
     * 更新密码
     * @param updateUserPasswordReqDTO
     * @return
     */
    Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 根据用户 ID 查询
     * @param findUserByIdReqDTO
     * @return
     */
    Response<FindUserByIdRespDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO);

    /**
     * 批量根据用户 ID 查询用户信息
     * @param findMultiUserByIdsReqDTO
     * @return
     */
    Response<List<FindMultiUserByIdsRespDTO>> findByIds(FindMultiUserByIdsReqDTO findMultiUserByIdsReqDTO);

}
