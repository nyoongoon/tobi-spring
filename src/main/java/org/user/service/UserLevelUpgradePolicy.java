package org.user.service;

import org.user.domain.User;

public interface UserLevelUpgradePolicy { // 직접 구현해보기...
    boolean canUpgradeLevel(User user);
    void upgradeLevel(User user);
}
