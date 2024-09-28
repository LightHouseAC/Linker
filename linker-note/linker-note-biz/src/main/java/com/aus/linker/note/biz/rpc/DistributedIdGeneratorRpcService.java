package com.aus.linker.note.biz.rpc;

import com.aus.linker.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * 生成雪花算法 ID
     * @return
     */
    public String getSnowflakeId() {
        return distributedIdGeneratorFeignApi.getSnowflakeId("leaf-test-snowflake");
    }

}
