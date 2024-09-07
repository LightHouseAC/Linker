package com.aus.framework.biz.context.config;

import com.aus.framework.biz.context.filter.HeaderUserId2ContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextAutoConfiguration {

    @Bean
    public FilterRegistrationBean<HeaderUserId2ContextFilter> filterRegistrationBean() {
        HeaderUserId2ContextFilter filter = new HeaderUserId2ContextFilter();
        FilterRegistrationBean<HeaderUserId2ContextFilter> bean = new FilterRegistrationBean<>(filter);
        return bean;
    }

}
