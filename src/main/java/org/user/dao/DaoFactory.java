package org.user.dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.user.service.UserService;

import javax.sql.DataSource;

@Configuration
public class DaoFactory {
    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

//        dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
        dataSource.setUrl("jdbc:mysql://localhost/springbook");
        dataSource.setUsername("spring");
        dataSource.setPassword("book");

        return dataSource;
    }

    @Bean
    public DataSourceTransactionManager transactionManager(){
        DataSourceTransactionManager dataSourceTransactionManager
                = new DataSourceTransactionManager(dataSource());
        return dataSourceTransactionManager;
    }

//    @Bean
//    public JdbcContext jdbcContext(){
//        JdbcContext jdbcContext = new JdbcContext();
//        jdbcContext.setDataSource(dataSource());
//        return jdbcContext;
//    }

    @Bean
    public UserDaoJdbc userDao() { //빈의 이름은 클래스 이름이 아니라 구현 클래스의 이름을 따름
        UserDaoJdbc userDaoJdbc = new UserDaoJdbc();
        userDaoJdbc.setDataSource(dataSource());
        return userDaoJdbc;
    }

    @Bean
    public UserService userService() {
        UserService userService = new UserService();
        userService.setUserDao(userDao());
        userService.setTransactionManager(transactionManager());
        return userService;
    }

    @Bean
    public JavaMailSenderImpl mailSender(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImple();
        mailSender.setHost("mail.server.com");
        return mailSender;
    }
}