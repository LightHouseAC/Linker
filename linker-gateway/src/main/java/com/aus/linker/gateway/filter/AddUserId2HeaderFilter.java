package com.aus.linker.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发请求时，将用户 ID 添加到 Header 请求头中，透传给下游服务
 */
@Component
@Slf4j
public class AddUserId2HeaderFilter implements GlobalFilter {

    /**
     * 请求头中，用户 ID 的键
     */
    private static final String HEADER_USER_ID = "userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("==================> TokenConvertFilter");

        // 用户 ID
        Long userId = null;
        try {
            // 获取当前登录用户的 ID
            userId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            // 若没有登录则直接放行
            return chain.filter(exchange);
        }

        log.info("## 当前登录的用户 ID：{}", userId);

        Long finalUserId = userId;
        ServerWebExchange newExchange = exchange.mutate() // 通过 mutate 方法创建一个新的 exchange 对象，用于修改 request
                .request(builder -> builder.header(HEADER_USER_ID, String.valueOf(finalUserId))) // 修改 request header，添加 userId
                .build();

        // 将请求传递给过滤器链中的下一个过滤器进行处理，不对请求进行任何修改
        return chain.filter(newExchange);
    }

}
