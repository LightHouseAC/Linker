package com.aus.linker.auth.domain.service.impl;

import com.aus.linker.auth.domain.dataobject.RolePermissionDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.auth.domain.dataobject.PermissionDO;
import com.aus.linker.auth.domain.service.PermissionService;
import com.aus.linker.auth.domain.mapper.PermissionDOMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author recww
* @description 针对表【t_permission(权限表)】的数据库操作Service实现
* @createDate 2024-08-22 20:07:10
*/
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionDOMapper, PermissionDO>
    implements PermissionService {

    @Override
    public List<PermissionDO> getAppEnabledList() {
        QueryWrapper<PermissionDO> wrapper = new QueryWrapper<>();
        wrapper.select("id", "name", "permission_key")
                .eq("status", 0).eq("type", 3);
        return list(wrapper);
    }
}




