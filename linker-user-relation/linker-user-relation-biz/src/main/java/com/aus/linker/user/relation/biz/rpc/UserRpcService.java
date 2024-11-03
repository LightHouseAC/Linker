package com.aus.linker.user.relation.biz.rpc;

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
     * 根据用户 ID 查询
     * @param userId
     * @return
     */
    public FindUserByIdRespDTO findUserById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);

        Response<FindUserByIdRespDTO> response = userFeignApi.findById(findUserByIdReqDTO);

        if (!response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }
        return response.getData();
    }

}
