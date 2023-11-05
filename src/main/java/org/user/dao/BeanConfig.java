package org.user.dao;

import org.learningtest.jdk.Message;
import org.learningtest.jdk.MessageFactoryBean;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.user.service.*;

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

    @Bean //부가기능(어드바이스)
    public TransactionAdvice transactionAdvice(){
        TransactionAdvice transactionAdvice = new TransactionAdvice();
        transactionAdvice.setTransactionManager(transactionManager());
        return transactionAdvice;
    }

    @Bean //포인트컷(메소드선정알고리즘)
    public NameMatchMethodPointcut transactionPointcut(){
        NameMatchMethodPointcut nameMatchMethodPointcut = new NameMatchMethodPointcut();
        nameMatchMethodPointcut.setMappedName("upgrade*");
        return nameMatchMethodPointcut;
    }

    @Bean // 어드바이스와 포인트컷을 담을 어드바이저 등록
    public DefaultPointcutAdvisor transactionAdvisor(){
        DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor();
        defaultPointcutAdvisor.setAdvice(transactionAdvice());
        defaultPointcutAdvisor.setPointcut(transactionPointcut());
        return defaultPointcutAdvisor;
    }

    @Bean // 타겟과 어드바이저를 담을 프록시 팩토리 빈 등록
    public ProxyFactoryBean userService(){
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(userServicImpl());
        proxyFactoryBean.setInterceptorNames("transactionAdvisor"); // 어드바이스와 어드바이저 동시 가능 설정. 리스트에 빈 아이디값 넣어줌.
        return proxyFactoryBean;
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
//    @Bean
//    public TxProxyFactoryBean userService(){
//        TxProxyFactoryBean txProxyFactoryBean = new TxProxyFactoryBean();
//        txProxyFactoryBean.setTarget(userServicImpl()); //핵심기능->핸들러의invoke()메소드에서 사용하기 위함
//        txProxyFactoryBean.setTransactionManager(transactionManager()); //부가기능 (어드바이스)
//        txProxyFactoryBean.setPattern("upgradeLevels"); // 메소드선정(포인트컷)
//        txProxyFactoryBean.setServiceInterface(UserService.class); //Class타입인 경우
//        return txProxyFactoryBean;
//    }

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