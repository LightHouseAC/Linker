package com.aus.linker.auth.controller;

import com.aus.framework.biz.operationlog.aspect.ApiOperationLog;
import com.aus.framework.common.response.Response;
import com.aus.linker.auth.domain.service.UserService;
import com.aus.linker.auth.model.vo.user.UserLoginReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    @ApiOperationLog(description = "用户登陆/注册")
    public Response<String> loginAndRegister(@Validated @RequestBody UserLoginReqVO userLoginReqVO){
        return userService.loginAndRegister(userLoginReqVO);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout(){
        return userService.logout();
    }

}
