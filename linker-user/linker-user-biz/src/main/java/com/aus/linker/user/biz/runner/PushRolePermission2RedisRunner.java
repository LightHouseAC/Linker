package com.aus.linker.user.biz.runner;

import cn.hutool.core.collection.CollUtil;
import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.user.biz.constant.RedisKeyConstants;
import com.aus.linker.user.biz.domain.dataobject.PermissionDO;
import com.aus.linker.user.biz.domain.dataobject.RoleDO;
import com.aus.linker.user.biz.domain.dataobject.RolePermissionDO;
import com.aus.linker.user.biz.domain.service.PermissionService;
import com.aus.linker.user.biz.domain.service.RolePermissionService;
import com.aus.linker.user.biz.domain.service.RoleService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PushRolePermission2RedisRunner implements ApplicationRunner {

    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RolePermissionService rolePermissionService;

    // 权限同步标记 Key
    public static final String PUSH_PERMISSION_FLAG = "push.permission.flag";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");

        try {
            // 是否能够同步数据：原子操作，只有key: PUSH_PERMISSION_FLAG 不存在时，才会设置该键的值为"1"，并且设置过期时间为1天
            boolean canPush = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(PUSH_PERMISSION_FLAG, "1", 1, TimeUnit.DAYS));
            // 如果无法同步权限数据
            if (!canPush) {
                log.warn("==> 角色权限数据已经同步至 Redis 中，不再同步!");
                return;
            }

            // 查询出所有角色
            List<RoleDO> roleDOS = roleService.getEnabledList();

            if (CollUtil.isNotEmpty(roleDOS)) {
                // 拿到所有角色的 ID
                List<Long> roleIds = roleDOS.stream().map(RoleDO::getId).toList();

                // 根据角色 ID，批量查询出所有角色对应的权限
                List<RolePermissionDO> rolePermissionDOS = rolePermissionService.getByRoleIds(roleIds);
                // 按角色 ID 分组，每个角色 ID 对应多个权限 ID
                Map<Long, List<Long>> roleIdPermissionIdsMap = rolePermissionDOS.stream().collect(
                        Collectors.groupingBy(RolePermissionDO::getRoleId,
                                Collectors.mapping(RolePermissionDO::getPermissionId, Collectors.toList()))
                );

                // 查询 APP 端所有被启用的权限
                List<PermissionDO> permissionDOS = permissionService.getAppEnabledList();
                // 权限 ID - 权限DO
                Map<Long, PermissionDO> permissionIdDOMap = permissionDOS.stream().collect(
                        Collectors.toMap(PermissionDO::getId, permissionDO -> permissionDO)
                );

                // 组织 角色-权限 关系
                Map<String, List<String>> roleKeyPermissionsMap = Maps.newHashMap();

                // 循环所有角色
                roleDOS.forEach(roleDO -> {
                    // 当前角色 ID
                    Long roleId = roleDO.getId();
                    // 当前角色 roleKey
                    String roleKey = roleDO.getRoleKey();
                    // 当前角色 ID 对应的权限 ID 集合
                    List<Long> permissionIds = roleIdPermissionIdsMap.get(roleId);
                    if (CollUtil.isNotEmpty(permissionIds)) {
                        List<String> permissionKeys = Lists.newArrayList();
                        permissionIds.forEach(permissionId -> {
                           // 根据权限 ID 获取具体的权限 DO 对象
                            PermissionDO permissionDO = permissionIdDOMap.get(permissionId);
                            permissionKeys.add(permissionDO.getPermissionKey());
                        });
                        roleKeyPermissionsMap.put(roleKey, permissionKeys);
                    }
                });

                // 同步至 Redis 中，方便后续网关查询鉴权使用
                roleKeyPermissionsMap.forEach((roleKey, permissions) -> {
                    String key = RedisKeyConstants.buildRolePermissionKey(roleKey);
                    redisTemplate.opsForValue().set(key, JsonUtil.toJsonString(permissions));
                });

            }

            log.info("==> 服务启动，成功同步角色权限到 Redis 中!");

        } catch (Exception e){

            log.error("==> 同步角色权限到 Redis 中失败：", e);

        }
    }

}
