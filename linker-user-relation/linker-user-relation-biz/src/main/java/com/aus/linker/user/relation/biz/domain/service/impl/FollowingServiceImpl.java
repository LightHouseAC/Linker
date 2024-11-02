package com.aus.linker.user.relation.biz.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.user.relation.biz.domain.dataobject.FollowingDO;
import com.aus.linker.user.relation.biz.domain.service.FollowingService;
import com.aus.linker.user.relation.biz.domain.mapper.FollowingDOMapper;
import org.springframework.stereotype.Service;

/**
* @author recww
* @description 针对表【t_following(用户关注表)】的数据库操作Service实现
* @createDate 2024-11-03 01:26:41
*/
@Service
public class FollowingServiceImpl extends ServiceImpl<FollowingDOMapper, FollowingDO>
    implements FollowingService {

}




