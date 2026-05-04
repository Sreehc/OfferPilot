package com.bytecoach.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.constant.SecurityConstants;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.mapper.UserMapper;
import com.bytecoach.user.service.UserService;
import com.bytecoach.user.vo.UserInfoVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User getByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    @Override
    public User createUser(String username, String password, String nickname) {
        User exists = getByUsername(username);
        if (exists != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(SecurityConstants.ROLE_USER);
        user.setStatus(1);
        user.setSource("system");
        user.setLastLoginTime(null);
        save(user);
        return user;
    }

    @Override
    public List<Long> searchUserIds(String keyword) {
        return lambdaQuery()
                .like(User::getUsername, keyword)
                .select(User::getId)
                .list()
                .stream()
                .map(User::getId)
                .toList();
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "user not found");
        }
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }
}
