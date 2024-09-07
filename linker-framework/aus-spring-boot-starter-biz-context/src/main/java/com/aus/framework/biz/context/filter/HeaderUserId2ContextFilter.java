package com.aus.framework.biz.context.filter;

import com.aus.framework.biz.context.holder.LoginUserContextHolder;
import com.aus.framework.common.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class HeaderUserId2ContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 从请求头中获取用户 ID
        String userId = request.getHeader(GlobalConstants.USER_ID);

        log.info("## HeaderUserId2ContextFilter，用户 Id: {}", userId);

        // 判断请求头中是否存在用户 ID
        if (StringUtils.isBlank(userId)) {
            // 若为空，则直接放行
            chain.doFilter(request, response);
            return;
        }

        // 如果 header 中存在 userId，则设置到 ThreadLocal 中
        log.info("===== 设置 userId 到 ThreadLocal 中，用户Id: {}", userId);
        LoginUserContextHolder.setUserId(userId);

        try {
            // 将请求和响应传递给过滤器链中的下一个过滤器
            chain.doFilter(request, response);
        } finally {
            // !!! 删除 ThreadLocal，防止内存泄漏
            LoginUserContextHolder.remove();
            log.info("===== 删除 ThreadLocal, userId: {}", userId);
        }

    }
}
