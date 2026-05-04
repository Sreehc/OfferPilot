package com.bytecoach.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.vo.UserInfoVO;
import java.util.List;

public interface UserService extends IService<User> {
    User getByUsername(String username);

    User createUser(String username, String password, String nickname);

    UserInfoVO getCurrentUserInfo();

    /**
     * Search user IDs by username keyword (for admin queries).
     */
    List<Long> searchUserIds(String keyword);
}

