package com.movie.interceptor;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import com.movie.entity.User;
import com.movie.utils.UserHolder;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @SuppressWarnings("FieldMayBeFinal")
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的 token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            response.setStatus(401); // 401 Unauthorized
            return false;
        }

        // 2. 基于 token 获取 redis 中的用户
        String key  = "login_token:" + token;
        String userJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(userJson)) {
            response.setStatus(401);
            return false;
        }

        // 3. 将查询到的 Hash 数据转为 User 对象
        User user = JSONUtil.toBean(userJson, User.class);

        // 4. 保存用户信息到 ThreadLocal
        UserHolder.saveUser(user);

        // 5. 刷新 token 有效期（用户只要在操作，token 就应该续期）
        stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
        
        // 6. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后，移除用户，防止内存泄漏
        UserHolder.removeUser();
    }
}