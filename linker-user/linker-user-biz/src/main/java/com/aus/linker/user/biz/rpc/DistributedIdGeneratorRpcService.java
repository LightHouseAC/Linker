package com.aus.linker.user.biz.rpc;

import com.aus.linker.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * Leaf Snowflake 模式：Linker ID 业务标识
     */
    private static final String BIZ_TAG_LINKER_ID = "leaf-snowflake-linker-id";

    /**
     * Leaf Snowflake 模式：Linker ID 业务标识
     */
    private static final String BIZ_TAG_USER_ID = "leaf-snowflake-user-id";

    /**
     * 调用分布式 ID 生成服务生成 用户 ID
     * @return
     */
    public String getLinkerId() {
        return distributedIdGeneratorFeignApi.getSnowflakeId(BIZ_TAG_LINKER_ID);
    }

    /**
     * 调用分布式 ID 生成服务生成 用户 ID
     * @return
     */
    public String getUserId() {
        return distributedIdGeneratorFeignApi.getSnowflakeId(BIZ_TAG_USER_ID);
    }

}
