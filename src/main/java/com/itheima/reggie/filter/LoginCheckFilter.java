package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经登录
 */
@WebFilter(urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //Spring 提供的工具类，用于匹配路径，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();
        String[] excludedUrls = {
                "/employee/login",
                "/employee/logout",
                "/backend/**", // 静态资源
                "/front/**", // 静态资源
        };
        if (requestIsExcluded(requestURI, excludedUrls) || request.getSession().getAttribute("employee") != null) {
            filterChain.doFilter(request, response);
            return;
        }
        response.getWriter().write(JSON.toJSONString(R.error("NOT LOGIN")));
    }

    public boolean requestIsExcluded(String requestURI, String[] excludeUrls) {
        for (String excludeUrl : excludeUrls) {
            if (PATH_MATCHER.match(excludeUrl, requestURI)) return true;
        }
        return false;
    }
}
