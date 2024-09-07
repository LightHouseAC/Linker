package com.aus.linker.gateway.auth;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenConfigure {
    // 注册 Sa-Token 全局过滤器
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")  /* 拦截全部path */
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    // 登录校验
                    SaRouter.match("/**") // 拦截所有路由
                            .notMatch("/auth/user/login") // 排除登录接口
                            .notMatch("/auth/verification/code/send") // 排除验证码发送接口
                            .check(r -> StpUtil.checkLogin());

                    // 权限认证 -- 不同模块，校验不同权限
                    SaRouter.match("/auth/user/logout", r -> StpUtil.checkPermission("app:note:publish"));
                })
                .setError(e -> {
                    // return SaResult.error(e.getMessage());
                    // 手动抛出异常，抛给全局异常处理器
                    if (e instanceof NotLoginException) { // 未登录异常
                        throw new NotLoginException(e.getMessage(), null, null);
                    } else if (e instanceof NotPermissionException) {
                        throw new NotPermissionException(e.getMessage());
                    } else {
                        throw new RuntimeException(e.getMessage());
                    }
                });
    }

}
