package com.aus.linker.count.biz.constant;

public class RedisConstants {

    /**
     * 用户维度计数 Key 前缀
     */
    private static final String COUNT_USER_KEY_PREFIX = "count:user:";

    /**
     * Hash Filed: 粉丝总数
     */
    public static final String FILED_FANS_TOTAL = "fansTotal";

    /**
     * 构建用户维度计数 Key
     * @param userId
     * @return
     */
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }

}