package org.user.dao;

import org.learningtest.jdk.Message;
import org.learningtest.jdk.MessageFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.user.service.TxProxyFactoryBean;
import org.user.service.UserService;
import org.user.service.UserServiceImpl;
import org.user.service.UserServiceTx;

import javax.sql.DataSource;

@Configuration
public class BeanConfig {
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

//    @Bean
//    public UserServiceTx userService (){
//        UserServiceTx userServiceTx = new UserServiceTx();
//        userServiceTx.setUserService(userServicImpl());
//        userServiceTx.setTransactionManager(transactionManager());
//    }
    @Bean
    public TxProxyFactoryBean userService(){
        TxProxyFactoryBean txProxyFactoryBean = new TxProxyFactoryBean();
        txProxyFactoryBean.setTarget(userServicImpl()); //핵심기능->핸들러의invoke()메소드에서 사용하기 위함
        txProxyFactoryBean.setTransactionManager(transactionManager());
        txProxyFactoryBean.setPattern("upgradeLevels");
        txProxyFactoryBean.setServiceInterface(UserService.class); //Class타입인 경우
        return txProxyFactoryBean;
    }

    @Bean
    public UserServiceImpl userServicImpl() {
        UserServiceImpl userServiceImpl = new UserServiceImpl();
        userServiceImpl.setUserDao(userDao());
        userServiceImpl.setMailSender(mailSender());
        return userServiceImpl;
    }


    @Bean
    public JavaMailSenderImpl mailSender(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImple();
        mailSender.setHost("mail.server.com");
        return mailSender;
    }

    @Bean
    public MessageFactoryBean message(){
        MessageFactoryBean messageFactoryBean = new MessageFactoryBean();
        messageFactoryBean.setText("Factory Bean");
        return messageFactoryBean;
    }
}