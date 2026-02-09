package com.movie.service.impl;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.movie.dto.UserLoginDTO;
import com.movie.dto.UserRegisterDTO;
import com.movie.entity.User;
import com.movie.entity.UserWallet;
import com.movie.mapper.UserMapper;
import com.movie.service.IUserService;
import com.movie.service.IUserWalletService;
import com.movie.vo.UserLoginVO;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;


/**
 * UserServiceImpl
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IUserWalletService userWalletService;

    @Override
    public void register(UserRegisterDTO dto) {
        // 1. 检查用户名是否已存在
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", dto.getUsername());
        if (count(query) > 0) {
            throw new RuntimeException("用户名已存在！");
        }

        // 2. 检查手机号
        QueryWrapper<User> phoneQuery = new QueryWrapper<>();
        phoneQuery.eq("phone", dto.getPhone());
        if (count(phoneQuery) > 0) {
            throw new RuntimeException("手机号已被注册！");
        }

        // 3. 创建用户对象
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        
        user.setStatus(true);     
        user.setIsAdmin(false); 
        
        // 4. 密码加密 (BCrypt)
        // 注意：如果这里报错，说明你没引 Hutool 包，或者 User 表没 password 字段
        String encodedPassword = BCrypt.hashpw(dto.getPassword());
        user.setPassword(encodedPassword);

        // 5. 保存
        save(user);

        UserWallet wallet = new UserWallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setPayPassword(null);
        userWalletService.save(wallet);
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        // 1. 根据用户名查询用户
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", dto.getUsername());
        User user = getOne(query);

        // 2. 校验用户是否存在
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (Boolean.FALSE.equals(user.getStatus())) {
            throw new RuntimeException("该账号已被禁用，请联系管理员");
        }

        // 3. 校验密码 (必须用 BCrypt.checkpw)
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 4. 生成 Token (用 UUID)
        String token = IdUtil.simpleUUID();
        String role = Boolean.TRUE.equals(user.getIsAdmin()) ? "admin" : "user";
        // 5. 把用户信息存入 Redis (Key: "login_token:xxxx", Value: UserJson, TTL: 24h)
        // 为什么存 Redis？因为这样后端就是无状态的，且查询极快
        String key = "login_token:" + token;
        // 把 User 对象转成 JSON 字符串
        String userJson = JSONUtil.toJsonStr(user);
        
        redisTemplate.opsForValue().set(key, userJson, 24, TimeUnit.HOURS);

        // 6. 组装返回给前端的数据
        UserLoginVO vo = new UserLoginVO();
        BeanUtil.copyProperties(user, vo); // 属性拷贝
        vo.setToken(token); // 塞入 Token
        vo.setRole(role);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(User user, String token) {
        // 1. 强制只允许修改特定字段 (安全过滤)
        // 防止用户恶意修改 id, username, balance, is_admin 等敏感字段
        User updateUser = new User();
        updateUser.setId(user.getId()); // ID 是必须的，用于 WHERE 条件
        
        // 只拷贝允许修改的字段
        updateUser.setNickname(user.getNickname());
        updateUser.setPhone(user.getPhone());
        updateUser.setEmail(user.getEmail());
        updateUser.setAvatarUrl(user.getAvatarUrl());
        
        // 2. 更新数据库
        this.updateById(updateUser);

        // 3. 【核心】同步更新 Redis
        // 因为拦截器是根据 token 去 Redis 拿用户的，如果 Redis 不更，拦截器拿到的永远是旧数据
        String key = "login_token:" + token;
        
        // 为了数据完整性，建议重新从数据库查一份最新的完整数据
        User latestUser = this.getById(user.getId());
        
        // 转 JSON
        String userJson = JSONUtil.toJsonStr(latestUser);
        
        // 更新 Redis，并重置过期时间 (例如 24小时)
        redisTemplate.opsForValue().set(key, userJson, 24, TimeUnit.HOURS);
    }
}