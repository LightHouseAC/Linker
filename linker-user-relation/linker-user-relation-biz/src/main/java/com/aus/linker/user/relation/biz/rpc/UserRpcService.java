package com.aus.linker.user.relation.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.aus.framework.common.response.Response;
import com.aus.linker.user.api.UserFeignApi;
import com.aus.linker.user.dto.req.FindMultiUserByIdsReqDTO;
import com.aus.linker.user.dto.req.FindUserByIdReqDTO;
import com.aus.linker.user.dto.resp.FindMultiUserByIdsRespDTO;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
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

    public List<FindMultiUserByIdsRespDTO> findByIds(List<Long> userIds) {
        FindMultiUserByIdsReqDTO findMultiUserByIdsReqDTO = new FindMultiUserByIdsReqDTO();
        findMultiUserByIdsReqDTO.setIds(userIds);

        Response<List<FindMultiUserByIdsRespDTO>> response = userFeignApi.findByIds(findMultiUserByIdsReqDTO);
        if (!response.isSuccess() || Objects.isNull(response.getData()) || CollUtil.isEmpty(response.getData())) {
            return null;
        }
        return response.getData();
    }

}
