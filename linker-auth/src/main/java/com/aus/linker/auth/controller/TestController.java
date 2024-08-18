package com.aus.linker.auth.controller;

import com.aus.framework.biz.operationlog.aspect.ApiOperationLog;
import com.aus.framework.common.response.Response;
import com.aus.linker.auth.domain.dataobject.User;
import com.aus.linker.auth.domain.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
public class TestController {

    @Resource
    private UserService userService;

    @PostMapping("/test")
    @ApiOperationLog(description = "测试接口")
    public Response<User> test(@RequestBody String name){
        User user = new User();
        user.setUsername(name);
        user.setCreateTime(LocalDateTime.now());
        userService.save(user);
        return Response.success(user);
    }

}
