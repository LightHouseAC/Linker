package com.aus.linker.auth.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.auth.domain.dataobject.User;
import com.aus.linker.auth.domain.service.UserService;
import com.aus.linker.auth.domain.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author recww
* @description 针对表【t_user(用户测试表)】的数据库操作Service实现
* @createDate 2024-08-18 15:59:12
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




