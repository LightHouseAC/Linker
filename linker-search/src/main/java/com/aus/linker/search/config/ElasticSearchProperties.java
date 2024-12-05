package com.aus.linker.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "elasticsearch")
@Component
@Data
public class ElasticSearchProperties {

    private String address;

}
