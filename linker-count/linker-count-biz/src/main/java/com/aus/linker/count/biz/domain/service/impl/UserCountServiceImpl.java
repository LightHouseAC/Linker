package com.aus.linker.count.biz.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.count.biz.domain.dataobject.UserCountDO;
import com.aus.linker.count.biz.domain.service.UserCountService;
import com.aus.linker.count.biz.domain.mapper.UserCountDOMapper;
import org.springframework.stereotype.Service;

/**
* @author lance.yang
* @description 针对表【t_user_count(用户计数表)】的数据库操作Service实现
* @createDate 2024-11-18 14:46:59
*/
@Service
public class UserCountServiceImpl extends ServiceImpl<UserCountDOMapper, UserCountDO>
    implements UserCountService {

}




