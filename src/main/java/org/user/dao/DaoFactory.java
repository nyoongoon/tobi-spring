package org.user.dao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.user.domain.User;

import javax.sql.DataSource;

@Configuration
public class DaoFactory {
    @Bean
    public DataSource dataSource(){
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

//        dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
        dataSource.setUrl("jdbc:mysql://localhost/springbook");
        dataSource.setUsername("spring");
        dataSource.setPassword("book");

        return dataSource;
    }

    @Bean
    public JdbcContext jdbcContext(){
        JdbcContext jdbcContext = new JdbcContext();
        jdbcContext.setDataSource(dataSource());
        return jdbcContext;
    }

    @Bean
    public UserDao userDao(){
        UserDao userDao = new UserDao();
        userDao.setJdbcContext(jdbcContext());
        return userDao;
    }

}