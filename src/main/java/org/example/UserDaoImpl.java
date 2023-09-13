package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.user.dao.JdbcContext;
import org.user.domain.User;

import java.sql.SQLException;

public class UserDaoImpl implements UserDaoEx{
    @Autowired
    JdbcContext context;
    @Override
    public void add(User user) {
        try{
            context.executeSql("select * from User;");
        }catch (SQLException e){
            // 여기서 다 잡으면 추상화 가능하긴 한데... //각기 다른 예외처리를 해서 추상화한 이유가 없어지는 걸까..
        }

    }
}
