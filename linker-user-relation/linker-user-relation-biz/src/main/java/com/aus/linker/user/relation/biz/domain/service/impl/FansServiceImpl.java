package com.aus.linker.user.relation.biz.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.user.relation.biz.domain.dataobject.FansDO;
import com.aus.linker.user.relation.biz.domain.service.FansService;
import com.aus.linker.user.relation.biz.domain.mapper.FansDOMapper;
import org.springframework.stereotype.Service;

/**
* @author recww
* @description 针对表【t_fans(用户粉丝表)】的数据库操作Service实现
* @createDate 2024-11-03 01:22:16
*/
@Service
public class FansServiceImpl extends ServiceImpl<FansDOMapper, FansDO>
    implements FansService {

}




