package org.user.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.user.service.DummyMailSender;
import org.user.service.UserService;
import org.user.service.UserServiceImpl;
import org.user.service.UserServiceTest;
import org.user.sqlservice.OxmSqlService;
import org.user.sqlservice.SqlRegistry;
import org.user.sqlservice.SqlService;
import org.user.sqlservice.updatable.EmbeddedDbSqlRegistry;

import javax.sql.DataSource;
import javax.xml.bind.Unmarshaller;
import java.sql.Driver;

@Configuration
//@ImportResource("/test-applicationContext.xml") 완전 대체함
@EnableTransactionManagement
@ComponentScan(basePackages="springbook.user")
public class TestApplicationContext {
//    @Autowired
//    SqlService sqlService;
    @Autowired
    UserDao userDao;

    @Bean
    public DataSource dataSource() { //인터페이스로 반환 주의
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource(); //구현체 클래스로 선언 주의
        dataSource.setDriverClass(Driver.class);
        dataSource.setUrl("jdbc:mysql://localhost/springbook?characterEncoding=UTF-8");
        dataSource.setUsername("spring");
        dataSource.setPassword("book");
        return dataSource;
    }
    @Bean
    public PlatformTransactionManager transactionManager(){
        DataSourceTransactionManager tm = new DataSourceTransactionManager();
        tm.setDataSource(dataSource());
        return tm;
    }
//    @Bean --> @Component로 대체
//    public UserDao userDao(){
//        UserDaoJdbc dao = new UserDaoJdbc();
//        dao.setDataSource(dataSource());
//        dao.setSqlService(sqlService()); // 아직 xml에 있을 때 컴파일 에러 어떻게 ?
//        return dao;
//    }
    @Bean
    public UserService userService(){
        UserServiceImpl service = new UserServiceImpl();
        service.setUserDao(this.userDao);
        service.setMailSender(mailSender());
        return service;
    }
    @Bean
    public UserService testUserService(){
        UserServiceTest.TestUserService testService = new UserServiceTest.TestUserService();
        testService.setUserDao(this.userDao);
        testService.setMailSender(mailSender());
        return testService;
    }
    @Bean
    public MailSender mailSender(){
        return new DummyMailSender();
    }
    @Bean
    public SqlService sqlService(){
        OxmSqlService sqlService = new OxmSqlService();
        sqlService.setUnmarshaller(unmarshaller());
        sqlService.setSqlRegistry(sqlRegistry());
        return sqlService;
    }
//    @Resource DataSource embeddedDatabase;
    @Bean
    public SqlRegistry sqlRegistry(){
        EmbeddedDbSqlRegistry sqlRegistry = new EmbeddedDbSqlRegistry();
        sqlRegistry.setDataSource(embeddedDatabase());
        return sqlRegistry;
    }
    @Bean
    public Unmarshaller unmarshaller(){
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("springbook.user.sqlservice.jaxb");
        return marshaller;
    }
    @Bean
    public DataSource embeddedDatabase(){
        return new EmbeddedDatabaseBuilder()
                .setName("embeddedDatabase")
                .setType(EmbeddedDatabaseType.HSQL)
                .addScript("classpath:springbook/user/sqlservice/updatable/sqlRegistrySchema.sql")
                .build();
    }
}
