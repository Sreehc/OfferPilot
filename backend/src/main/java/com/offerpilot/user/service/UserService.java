package com.offerpilot.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.offerpilot.user.entity.User;
import com.offerpilot.user.vo.UserInfoVO;
import java.util.List;

public interface UserService extends IService<User> {
    User getByUsername(String username);

    User getByEmail(String email);

    User createUser(String username, String password, String nickname, String email);

    void updatePassword(Long userId, String rawPassword);

    UserInfoVO getCurrentUserInfo();

    /**
     * Search user IDs by username keyword (for admin queries).
     */
    List<Long> searchUserIds(String keyword);
}
