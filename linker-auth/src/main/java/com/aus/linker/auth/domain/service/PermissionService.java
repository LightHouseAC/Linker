package com.aus.linker.auth.domain.service;

import com.aus.linker.auth.domain.dataobject.PermissionDO;
import com.aus.linker.auth.domain.dataobject.RolePermissionDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author recww
* @description 针对表【t_permission(权限表)】的数据库操作Service
* @createDate 2024-08-22 20:07:10
*/
public interface PermissionService extends IService<PermissionDO> {

    /**
     * 查询 APP 端所有被启用的权限
     * @return
     */
    List<PermissionDO> getAppEnabledList();

}
