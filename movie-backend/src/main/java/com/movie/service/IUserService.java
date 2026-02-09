package com.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.dto.UserLoginDTO;
import com.movie.dto.UserRegisterDTO;
import com.movie.entity.User;
import com.movie.vo.UserLoginVO;

/**
 * 用户服务接口
 */
public interface IUserService extends IService<User> {

    /**
     * 用户注册
     * @param dto 注册参数
     */
    void register(UserRegisterDTO dto);
    
    UserLoginVO login(UserLoginDTO dto);
    void updateUserInfo(User user, String token);
}