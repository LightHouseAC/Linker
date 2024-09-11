package com.aus.linker.user.biz.domain.service;

import com.aus.linker.user.biz.domain.dataobject.RolePermissionDO;
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
