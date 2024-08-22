package com.aus.linker.auth.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.auth.domain.dataobject.RoleDO;
import com.aus.linker.auth.domain.service.RoleService;
import com.aus.linker.auth.domain.mapper.RoleDOMapper;
import org.springframework.stereotype.Service;

/**
* @author recww
* @description 针对表【t_role(角色表)】的数据库操作Service实现
* @createDate 2024-08-22 20:10:16
*/
@Service
public class RoleServiceImpl extends ServiceImpl<RoleDOMapper, RoleDO>
    implements RoleService {

}




