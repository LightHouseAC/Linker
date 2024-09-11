package com.aus.linker.user.biz.domain.service;

import com.aus.linker.user.biz.domain.dataobject.PermissionDO;
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
