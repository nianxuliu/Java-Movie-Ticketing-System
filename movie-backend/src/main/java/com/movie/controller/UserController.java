package com.movie.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.dto.UserLoginDTO;
import com.movie.dto.UserRegisterDTO;
import com.movie.entity.User;
import com.movie.service.IUserService;
import com.movie.utils.UserHolder;
import com.movie.vo.UserLoginVO;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @PostMapping("/register")
    public Result<String> register(@RequestBody UserRegisterDTO dto) {
        try {
            userService.register(dto);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO dto) {
        try {
            UserLoginVO vo = userService.login(dto);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 修改个人信息
    @PutMapping("/update")
    public Result<String> updateInfo(@RequestBody User user, HttpServletRequest request) {
        // 1. 获取当前登录用户 ID
        Long currentUserId = UserHolder.getUser().getId();
        
        // 2. 强制设置 ID，防止恶意修改他人数据
        user.setId(currentUserId);
        
        // 3. 获取 Token (用于更新 Redis)
        String token = request.getHeader("authorization");
        
        try {
            userService.updateUserInfo(user, token);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error("修改失败: " + e.getMessage());
        }
    }
    
    // 获取当前用户信息 (用于前端回显)
    @GetMapping("/me")
    public Result<User> getMe() {
        Long userId = UserHolder.getUser().getId();
        User user = userService.getById(userId);
        if(user != null) {
            user.setPassword(null); // 脱敏，不返回密码
        }
        return Result.success(user);
    }

    // 封号/解封用户 (status: 0禁用 1正常)
    @SysLog("修改用户状态")
    @PutMapping("/status/{userId}/{status}")
    public Result<String> updateStatus(@PathVariable Long userId, @PathVariable Integer status) {
        if (status != 0 && status != 1) {
            return Result.error("状态不合法");
        }
        
        User user = new User();
        user.setId(userId);
        
        // --- 修复：Integer 转 Boolean ---
        // 假设 1 是正常(true)，0 是禁用(false)
        user.setStatus(status == 1); 
        
        userService.updateById(user);
        
        return Result.success(status == 1 ? "已解封" : "已封号");
    }

    @GetMapping("/list")
    public Result<IPage<User>> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String username) {
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> query = new QueryWrapper<>();
        
        // 支持按用户名模糊搜索
        if (username != null && !username.isEmpty()) {
            query.like("username", username);
        }
        
        query.orderByDesc("create_time"); // 新注册的在前面
        
        return Result.success(userService.page(pageParam, query));
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        Long userId = UserHolder.getUser().getId();

        User user = userService.getById(userId);
        if (!user.getPassword().equals(oldPassword)) {
            return Result.error("原密码错误");
        }

        user.setPassword(newPassword);
        userService.updateById(user);
        return Result.success("密码修改成功");
    }
}
