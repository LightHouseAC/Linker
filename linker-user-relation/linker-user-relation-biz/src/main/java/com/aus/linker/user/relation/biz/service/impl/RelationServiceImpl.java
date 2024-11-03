package com.aus.linker.user.relation.biz.service.impl;

import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.linker.user.dto.resp.FindUserByIdRespDTO;
import com.aus.linker.user.relation.biz.enums.ResponseCodeEnum;
import com.aus.linker.user.relation.biz.model.vo.FollowUserReqVO;
import com.aus.linker.user.relation.biz.rpc.UserRpcService;
import com.aus.linker.user.relation.biz.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private UserRpcService userRpcService;

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 关注的用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();

        // 当前登录的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 校验：无法关注自己
        if (Objects.equals(userId, followUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOURSELF);
        }

        // TODO: 校验关注用户是否存在
        FindUserByIdRespDTO findUserByIdRespDTO = userRpcService.findUserById(followUserId);

        if (Objects.isNull(findUserByIdRespDTO)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTS);
        }

        // TODO: 校验关注数是否已到上限

        // TODO: 写入 Redis ZSET 关注列表

        // TODO: 发送 MQ

        return Response.success();
    }
}
