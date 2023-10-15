package org.user.service;

import org.user.domain.User;

public interface UserService {
    void add(User user);
    void upgradeLevels();
}
