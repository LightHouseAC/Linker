package com.aus.linker.auth.rpc;

import com.aus.framework.common.response.Response;
import com.aus.linker.user.api.UserFeignApi;
import com.aus.linker.user.dto.req.FindUserByPhoneReqDTO;
import com.aus.linker.user.dto.req.RegisterUserReqDTO;
import com.aus.linker.user.dto.req.UpdateUserPasswordReqDTO;
import com.aus.linker.user.dto.resp.FindUserByPhoneRespDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 用户注册
     * @param phone
     * @return
     */
    public Long registerUser(String phone) {
        RegisterUserReqDTO registerUserReqDTO = new RegisterUserReqDTO();
        registerUserReqDTO.setPhone(phone);

        Response<Long> response = userFeignApi.registerUser(registerUserReqDTO);

        if (!response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    /**
     * 根据手机号查询用户信息
     * @param phone
     * @return
     */
    public FindUserByPhoneRespDTO findUserByPhone(String phone) {

        FindUserByPhoneReqDTO findUserByPhoneReqDTO = new FindUserByPhoneReqDTO();
        findUserByPhoneReqDTO.setPhoneNumber(phone);

        Response<FindUserByPhoneRespDTO> response = userFeignApi.findByPhone(findUserByPhoneReqDTO);

        if (!response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    /**
     * 密码更新
     * @param encodePassword
     */
    public void updatePassword(String encodePassword) {
        UpdateUserPasswordReqDTO updateUserPasswordReqDTO = new UpdateUserPasswordReqDTO();
        updateUserPasswordReqDTO.setEncodePassword(encodePassword);
        userFeignApi.updatePassword(updateUserPasswordReqDTO);
    }

}
