package com.aus.linker.user.biz.domain.service;

import com.aus.linker.user.biz.domain.dataobject.RoleDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author recww
* @description 针对表【t_role(角色表)】的数据库操作Service
* @createDate 2024-08-22 20:10:16
*/
public interface RoleService extends IService<RoleDO> {

    /**
     * 查询所有被启用的角色
     * @return
     */
    List<RoleDO> getEnabledList();

}