package com.movie.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import com.movie.entity.User;
import com.movie.utils.UserHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从 ThreadLocal 获取用户信息 (LoginInterceptor 已经塞进去了)
        User user = UserHolder.getUser();

        // 2. 检查用户是否存在，以及是否是管理员
        if (user == null || Boolean.FALSE.equals(user.getIsAdmin())) {
            response.setStatus(403); // 403 Forbidden - 你有身份，但没权限
            return false;
        }

        // 3. 是管理员，放行
        return true;
    }
}