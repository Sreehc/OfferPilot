package com.offerpilot.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.constant.SecurityConstants;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import com.offerpilot.user.entity.User;
import com.offerpilot.user.mapper.UserMapper;
import com.offerpilot.user.service.UserService;
import com.offerpilot.user.vo.UserInfoVO;
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
    public User getByEmail(String email) {
        return lambdaQuery().eq(User::getEmail, email).one();
    }

    @Override
    public User createUser(String username, String password, String nickname, String email) {
        User exists = getByUsername(username);
        if (exists != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "username already exists");
        }
        if (getByEmail(email) != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "email already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setEmailVerified(Boolean.FALSE);
        user.setEmailVerifiedAt(null);
        user.setRole(SecurityConstants.ROLE_USER);
        user.setStatus(1);
        user.setSource("system");
        user.setLastLoginTime(null);
        save(user);
        return user;
    }

    @Override
    public void updatePassword(Long userId, String rawPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "user not found");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        updateById(user);
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
                .email(user.getEmail())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .githubLinked(user.getGithubId() != null && !user.getGithubId().isBlank())
                .githubUsername(user.getGithubUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }
}
