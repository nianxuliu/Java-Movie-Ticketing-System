package com.movie.aspect;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.movie.annotation.SysLog;
import com.movie.entity.Log;
import com.movie.entity.User;
import com.movie.service.ILogService;
import com.movie.utils.UserHolder;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private ILogService logService;

    @Around("@annotation(com.movie.annotation.SysLog)")
    public Object saveLog(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();

        // 1. 执行目标方法
        Object result = point.proceed();

        // 2. 计算执行时长
        long time = System.currentTimeMillis() - beginTime;

        // 3. 异步保存日志
        recordLog(point, time);

        return result;
    }

    private void recordLog(ProceedingJoinPoint point, long time) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        Log sysLog = new Log();

        // --- 1. 设置操作内容 (Action) ---
        SysLog logAnnotation = method.getAnnotation(SysLog.class);
        if (logAnnotation != null) {
            sysLog.setAction(logAnnotation.value());
        }

        // --- 2. 设置方法名 (Method) ---
        String className = point.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLog.setMethod(className + "." + methodName + "()");

        // --- 3. 设置参数 (Params) ---
        // 使用 Hutool 工具类序列化参数，并截取过长内容防止数据库报错
        try {
            Object[] args = point.getArgs();
            String params = JSONUtil.toJsonStr(args);
            if (params.length() > 500) {
                params = params.substring(0, 500) + "...";
            }
            sysLog.setParams(params);
        } catch (Exception e) {
            // 参数解析失败不影响日志记录
        }

        // --- 4. 设置操作人 (Username) ---
        User user = UserHolder.getUser();
        if (user != null) {
            sysLog.setUsername(user.getUsername());
        } else {
            sysLog.setUsername("游客/未登录");
        }

        // --- 5. 设置 IP 地址 (核心优化) ---
        // 获取 Request 对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = getIpAddr(request); // 使用增强版 IP 获取方法
            sysLog.setIp(ip);
        }

        // --- 6. 设置耗时 (Time) - 对应你的数据库字段 ---
        sysLog.setTime(time); 

        // --- 7. 设置创建时间 ---
        sysLog.setCreateTime(LocalDateTime.now());

        // 保存到数据库
        logService.save(sysLog);
    }

    /**
     * 获取客户端真实IP地址 (处理代理和本地IPv6问题)
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理本地开发环境的 IPv6 地址 0:0:0:0:0:0:0:1 转为 127.0.0.1
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}