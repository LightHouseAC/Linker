package com.aus.linker.user.biz.constant;

public class RedisKeyConstants {

    /**
     * 全局自增ID生成器
     */
    public static final String LINKER_ID_GENERATOR_KEY = "linker.id.generator";

    /**
     * 用户角色数据 KEY 前缀
     */
    public static final String USER_ROLE_KEY_PREFIX = "user:roles:";

    /**
     * 角色对应的权限集合 KEY 前缀
     */
    public static final String ROLE_PERMISSION_KEY_PREFIX = "role:permissions:";

    public static String buildUserRoleKey(Long userId) {
        return USER_ROLE_KEY_PREFIX + userId;
    }

    /**
     * 构建角色对应的权限集合 KEY
     * @param roleKey
     * @return
     */
    public static String buildRolePermissionKey(String roleKey) {
        return ROLE_PERMISSION_KEY_PREFIX + roleKey;
    }

}