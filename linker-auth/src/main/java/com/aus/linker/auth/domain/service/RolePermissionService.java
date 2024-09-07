package com.aus.linker.auth.domain.service;

import com.aus.linker.auth.domain.dataobject.RolePermissionDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RolePermissionService extends IService<RolePermissionDO> {

    /**
     * 根据角色 ID 集合批量查询
     * @param roleIds
     * @return
     */
    List<RolePermissionDO> getByRoleIds(List<Long> roleIds);

}
