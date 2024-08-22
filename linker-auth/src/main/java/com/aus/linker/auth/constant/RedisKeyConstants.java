package com.aus.linker.auth.constant;

public class RedisKeyConstants {

    /**
     * 验证码key前缀
     */
    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";

    /**
     * 全局自增ID生成器
     */
    public static final String LINKER_ID_GENERATOR_KEY = "linker_id_generator";

    /**
     * 用户角色数据 KEY 前缀
     */
    public static final String USER_ROLE_KEY_PREFIX = "user:roles:";

    /**
     * 构建验证码key
     * @param phone
     * @return
     */
    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

    public static String buildUserRoleKey(String phone) {
        return USER_ROLE_KEY_PREFIX + phone;
    }

}