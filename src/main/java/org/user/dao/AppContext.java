package org.user.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.user.service.DummyMailSender;
import org.user.service.UserService;
import org.user.service.UserServiceTest;
import org.user.sqlservice.*;
import org.user.sqlservice.annotation.EnableSqlService;
import org.user.sqlservice.updatable.EmbeddedDbSqlRegistry;

import javax.sql.DataSource;
import javax.xml.bind.Unmarshaller;
import java.sql.Driver;

@Configuration
//@ImportResource("/test-applicationContext.xml") 완전 대체함
@EnableTransactionManagement
@ComponentScan(basePackages = "springbook.user")
//@Import({SqlServiceContext.class, AppContext.TestAppContext.class, AppContext.ProductionAppContext.class})
//@Import(SqlServiceContext.class) //@Import를 메타애노테이션으로 사용하는 커스텀 어노테이션을 생성..
@EnableSqlService
@PropertySource("/database.properties")
//public class TestApplicationContext { --> 테스트 정보는 분리함
public class AppContext implements SqlMapConfig{
    //    @Autowired
//    SqlService sqlService;
    @Autowired
    UserDao userDao;
    @Autowired
    Environment env;

    @Value("${db.driverClass}") Class<? extends Driver> driverClass;
    @Value("${db.url}") String url;
    @Value("${db.username}") String username;
    @Value("${db.password}") String pasword;

    // 프로퍼티 소스를 이용한 치환자 설정용 빈
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer(){
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Override
    public Resource getSqlMapResource(){
        return new ClassPathResource("sqlmap.xml", UserDao.class);
    }

//    @Bean
//    public SqlMapConfig sqlMapConfig(){ //@AppContext가 SqlMapConfig를 직접구현하여 필요 없어짐..
//        return new UserSqlMapConfig();
//    }

    @Bean
    public DataSource dataSource() { //인터페이스로 반환 주의
//        SimpleDriverDataSource dataSource = new SimpleDriverDataSource(); //구현체 클래스로 선언 주의
//        dataSource.setDriverClass(Driver.class);
//        dataSource.setUrl("jdbc:mysql://localhost/springbook?characterEncoding=UTF-8");
//        dataSource.setUsername("spring");
//        dataSource.setPassword("book");
//        return dataSource;
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
//        try {
//            ds.setDriverClass((Class<? extends java.sql.Driver>) Class.forName(env.getProperty("db.driverClass")));
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        ds.setUrl(env.getProperty("db.url"));
//        ds.setUsername(env.getProperty("db.username"));
//        ds.setPassword(env.getProperty("db.password"));
        ds.setDriverClass(this.driverClass);
        ds.setUrl(this.url);
        ds.setUsername(this.username);
        ds.setPassword(this.pasword);
        return ds;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        DataSourceTransactionManager tm = new DataSourceTransactionManager();
        tm.setDataSource(dataSource());
        return tm;
    }

    @Bean
    public MailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("mail.mycompany.com");
        return mailSender;
    }

//    @Bean --> @Component로 대체
//    public UserDao userDao(){
//        UserDaoJdbc dao = new UserDaoJdbc();
//        dao.setDataSource(dataSource());
//        dao.setSqlService(sqlService()); // 아직 xml에 있을 때 컴파일 에러 어떻게 ?
//        return dao;
//    }
//    @Bean --> @Component로 대체
//    public UserService userService(){
//        UserServiceImpl service = new UserServiceImpl();
//        service.setUserDao(this.userDao);
//        service.setMailSender(mailSender());
//        return service;
//    }
//    @Bean
//    public UserService testUserService(){ --> TestAppContext
//        UserServiceTest.TestUserService testService = new UserServiceTest.TestUserService();
//        testService.setUserDao(this.userDao);
//        testService.setMailSender(mailSender());
//        return testService;
//    }
//    @Bean
//    public MailSender mailSender(){ --> TestAppContext
//        return new DummyMailSender();
//    }

//    @Bean --> SqlServiceContext
//    public SqlService sqlService(){
//        OxmSqlService sqlService = new OxmSqlService();
//        sqlService.setUnmarshaller(unmarshaller());
//        sqlService.setSqlRegistry(sqlRegistry());
//        return sqlService;
//    }
////    @Resource DataSource embeddedDatabase;
//    @Bean
//    public SqlRegistry sqlRegistry(){
//        EmbeddedDbSqlRegistry sqlRegistry = new EmbeddedDbSqlRegistry();
//        sqlRegistry.setDataSource(embeddedDatabase());
//        return sqlRegistry;
//    }
//    @Bean
//    public Unmarshaller unmarshaller(){
//        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
//        marshaller.setContextPath("springbook.user.sqlservice.jaxb");
//        return marshaller;
//    }
//    @Bean
//    public DataSource embeddedDatabase(){
//        return new EmbeddedDatabaseBuilder()
//                .setName("embeddedDatabase")
//                .setType(EmbeddedDatabaseType.HSQL)
//                .addScript("classpath:springbook/user/sqlservice/updatable/sqlRegistrySchema.sql")
//                .build();
//    }

    @Configuration
    @Profile("production")
    public static class ProductionAppContext {
        @Bean
        public MailSender mailSender() {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost("localhost");
            return mailSender;
        }
    }

    @Configuration
    @Profile("test")
    public static class TestAppContext {
        @Bean
        public UserService testUserService() {
            return new UserServiceTest.TestUserService();
        }

        @Bean
        public MailSender mailSender() {
            return new DummyMailSender();
        }
    }
}
