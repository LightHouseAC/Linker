package com.aus.linker.auth.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.auth.domain.dataobject.RoleDO;
import com.aus.linker.auth.domain.service.RoleService;
import com.aus.linker.auth.domain.mapper.RoleDOMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author recww
* @description 针对表【t_role(角色表)】的数据库操作Service实现
* @createDate 2024-08-22 20:10:16
*/
@Service
public class RoleServiceImpl extends ServiceImpl<RoleDOMapper, RoleDO>
    implements RoleService {

    @Override
    public List<RoleDO> getEnabledList() {
        QueryWrapper<RoleDO> wrapper = new QueryWrapper<>();
        wrapper.select("id", "role_key", "role_name")
                .eq("status", 0);
        return list(wrapper);
    }

}




