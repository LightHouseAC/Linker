package com.aus.linker.oss.factory;

import com.aus.linker.oss.strategy.FileStrategy;
import com.aus.linker.oss.strategy.impl.AliyunFileStrategy;
import com.aus.linker.oss.strategy.impl.MinioFileStrategy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class FileStrategyFactory {

    @Value("${storage.type}")
    private String strategyType;

    @Value("${storage.bucket}")
    private String bucket;

    @Bean
    @RefreshScope
    public FileStrategy getFileStrategy() {
        if (StringUtils.equals(strategyType, "minio")) {
            return new MinioFileStrategy(bucket);
        } else if (StringUtils.equals(strategyType, "aliyun-oss")) {
            return new AliyunFileStrategy(bucket);
        }

        throw new IllegalArgumentException("不可用的存储类型");
    }

}