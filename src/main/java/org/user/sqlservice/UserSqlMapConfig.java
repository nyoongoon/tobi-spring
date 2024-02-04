package org.user.sqlservice;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.user.dao.UserDao;

public class UserSqlMapConfig implements SqlMapConfig{ // @AppConfig가 SqlMapConfig를 직접 구현하여 이 클래스는 필요없어짐..
    @Override
    public Resource getSqlMapResource() {
        return new ClassPathResource("sqlmap.xml", UserDao.class);
    }
}
