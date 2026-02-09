package com.movie.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.movie.interceptor.AdminAuthInterceptor;
import com.movie.interceptor.LoginInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .addPathPatterns("/**") // 拦截所有
                .excludePathPatterns(   // --- 排除所有公开路径 ---
                        // 用户认证
                        "/user/login",
                        "/user/register",
                        // 文件上传
                        "/file/upload",
                        // 公开内容查询
                        "/movie/search",     // ES 搜索
                        "/movie/top10",      // 热门电影
                        "/movie/detail/**",  // 电影详情
                        "/actorInfo/list",       // 演员列表
                        "/actorInfo/{id}",       // 演员详情
                        "/directorInfo/list",    // 导演列表
                        "/directorInfo/{id}",    // 导演详情
                        "/schedule/list/**", // 某电影的排片
                        "/review/list/**",   // 某电影的评论
                        "/reply/list/**",
                        "/cinema/list",
                        "/hall/list/**"     // 某评论的回复
                ).order(1);

        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns(
                    // --- 把所有管理员接口都加到这里 ---
                    "/movie/add", "/movie/update", "/movie/delete/**",
                    "/actor/**", "/director/**", 
                    "/cinema/**", "/hall/**", "/schedule/**",
                    "/report/**", "/log/**",
                    "/review/delete/**", "/review/admin/**",
                    "/user/status/**",
                    "/reply/delete/**"
                )
                .excludePathPatterns(
            "/schedule/list/**",
                        "/cinema/list/**" ,// 允许普通用户访问排片列表，不触发管理员检查
                        "/hall/list/**"  // 允许普通用户访问影厅列表，不触发管理员检查
                )
                .order(2);  
    }
}