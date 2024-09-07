package com.aus.linker.auth.controller;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.aus.linker.auth.alarm.AlarmInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @NacosValue(value = "${rate-limit.api.limit}", autoRefreshed = true)
    private Integer limit;

    @Resource
    private AlarmInterface alarm;

    @GetMapping("/rateLimit")
    public Integer getRateLimit() {
        return limit;
    }

    @GetMapping("/alarm")
    public String sendAlarm() {
        alarm.send("系统出错了！");
        return "alarm success";
    }

}
