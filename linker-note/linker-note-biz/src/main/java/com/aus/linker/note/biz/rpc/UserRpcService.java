package com.aus.linker.note.biz.rpc;

import com.aus.framework.common.response.Response;
import com.aus.linker.user.api.UserFeignApi;
import com.aus.linker.user.dto.req.FindUserByIdReqDTO;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 查询用户信息
     * @param userId
     * @return
     */
    public FindUserByIdRespDTO findById(Long userId){
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);

        Response<FindUserByIdRespDTO> response = userFeignApi.findById(findUserByIdReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()){
            return null;
        }
        return response.getData();
    }

}
