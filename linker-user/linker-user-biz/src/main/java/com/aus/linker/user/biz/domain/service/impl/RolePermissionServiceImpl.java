package com.aus.linker.user.biz.domain.service.impl;

import com.aus.linker.user.biz.domain.dataobject.RolePermissionDO;
import com.aus.linker.user.biz.domain.mapper.RolePermissionDOMapper;
import com.aus.linker.user.biz.domain.service.RolePermissionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionDOMapper, RolePermissionDO>
        implements RolePermissionService {

    @Override
    public List<RolePermissionDO> getByRoleIds(List<Long> roleIds) {
        QueryWrapper<RolePermissionDO> wrapper = new QueryWrapper<>();
        wrapper.select("role_id", "permission_id")
                .in("role_id", roleIds);
        return list(wrapper);
    }

}
