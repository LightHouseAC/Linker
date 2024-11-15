package com.aus.linker.user.api;

import com.aus.framework.common.response.Response;
import com.aus.linker.user.constant.ApiConstants;
import com.aus.linker.user.dto.req.*;
import com.aus.linker.user.dto.resp.FindMultiUserByIdsRespDTO;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.aus.linker.user.dto.resp.FindUserByPhoneRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {

    String PREFIX = "/user";

    /**
     * 用户注册
     * @param registerUserReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/register")
    Response<Long> registerUser(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据手机号查询信息
     * @param findUserByPhoneReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/findByPhone")
    Response<FindUserByPhoneRespDTO> findByPhone(@RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    /**
     * 更新密码
     * @param updateUserPasswordReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 根据用户 ID 查询用户信息
     * @param findUserByIdReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/findById")
    Response<FindUserByIdRespDTO> findById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

    /**
     * 批量查询用户信息
     * @param findMultiUserByIdsReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/findByIds")
    Response<List<FindMultiUserByIdsRespDTO>> findByIds(@RequestBody FindMultiUserByIdsReqDTO findMultiUserByIdsReqDTO);

}
