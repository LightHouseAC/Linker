package com.aus.linker.user.biz.domain.service.impl;

import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.response.Response;
import com.aus.framework.common.utils.ParamUtils;
import com.aus.linker.oss.api.FileFeignApi;
import com.aus.linker.user.biz.domain.dataobject.UserDO;
import com.aus.linker.user.biz.domain.mapper.UserDOMapper;
import com.aus.linker.user.biz.domain.service.UserService;
import com.aus.linker.user.biz.enums.ResponseCodeEnum;
import com.aus.linker.user.biz.enums.SexEnum;
import com.aus.linker.user.biz.model.vo.UpdateUserInfoReqVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
* @author recww
* @description 针对表【t_user(用户表)】的数据库操作Service实现
* @createDate 2024-09-07 21:58:57
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserDOMapper, UserDO>
    implements UserService {

    @Resource
    private FileFeignApi fileFeignApi;

    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = new UserDO();
        // 设置当前需要更新的用户 ID
        userDO.setId(LoginUserContextHolder.getUserId());
        // 标识：是否需要更新
        boolean needUpdate = false;

        // 头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();

        if (Objects.nonNull(avatarFile)) {
            // todo：调用对象存储服务上传文件
            fileFeignApi.test();
        }

        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickName(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL);
            userDO.setNickname(nickname);
            needUpdate = true;
        }

        // Linker 号
        String linkerId = updateUserInfoReqVO.getLinkerId();
        if (StringUtils.isNotBlank(linkerId)) {
            Preconditions.checkArgument(ParamUtils.checkLinkerId(linkerId), ResponseCodeEnum.LINKER_ID_VALID_FAIL);
            userDO.setLinkerId(linkerId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL);
            userDO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL);
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图
        MultipartFile backgroundImg = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImg)) {
            // todo: 调用对象存储服务上传文件
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            updateById(userDO);
        }

        return Response.success();
    }
}




