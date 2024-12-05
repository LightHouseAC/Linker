package com.aus.linker.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class ElasticSearchRestHighLevelClient {

    @Resource
    private ElasticSearchProperties elasticSearchProperties;

    private static final String COLON = ":";
    private static final String HTTP = "http";

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        String address = elasticSearchProperties.getAddress();

        // 按冒号 ： 分隔
        String[] addressArr = address.split(COLON);
        // IP 地址
        String host = addressArr[0];
        // 端口
        int port = Integer.parseInt(addressArr[1]);

        HttpHost httpHost = new HttpHost(host, port, HTTP);

        return new RestHighLevelClient(RestClient.builder(httpHost));

    }

}
