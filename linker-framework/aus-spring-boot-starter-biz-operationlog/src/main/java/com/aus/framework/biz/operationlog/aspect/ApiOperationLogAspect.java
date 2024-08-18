package com.aus.framework.biz.operationlog.aspect;

import com.aus.framework.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@Slf4j
public class ApiOperationLogAspect {

    /** 定义切点为@ApiOperationLog注解，该注解声明了类型为运行时注解，且作用于方法 **/
    @Pointcut("@annotation(com.aus.framework.biz.operationlog.aspect.ApiOperationLog)")
    public void apiOperationLog(){}

    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 请求开始时间
        long startTime = System.currentTimeMillis();

        // 获取被请求的类和方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 请求的入参
        Object[] args = joinPoint.getArgs();
        // 入参转Json
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(", "));

        // 功能描述信息
        String description = getApiOperationLogDescription(joinPoint);

        // 打印请求相关参数
        log.info("======> 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} ======",
                description, argsJsonStr, className, methodName);

        // 执行切点方法
        Object result = joinPoint.proceed();

        // 执行耗时
        long executionTime = System.currentTimeMillis() - startTime;

        // 打印出参等相关信息
        log.info("======> 请求结束: [{}], 耗时: {}ms, 出参: {} ======",
                description, executionTime, JsonUtil.toJsonString(result));

        return result;
    }

    /**
     * 从切入点获取获取该注解的描述信息
     * @param joinPoint
     * @return
     */
    private String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        // 1. 从切入点获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 2. 使用签名获取当前被注解的方法
        Method method = methodSignature.getMethod();
        // 3. 从方法中提取注解
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);
        // 4. 从注解中获取description属性
        return apiOperationLog.description();
    }

    /**
     * 转Json字符串
     * @return
     */
    private Function<Object, String> toJsonStr() {
        return JsonUtil::toJsonString;
    }

}
