package org.user.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.user.dao.AppContext;
import org.user.dao.Level;
import org.user.dao.UserDao;
import org.user.dao.UserDaoJdbc;
import org.user.domain.User;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import static org.junit.Assert.fail;
import static org.user.service.UserService.MIN_LOGCOUNT_FOR_SILVER;
import static org.user.service.UserService.MIN_RECCOMEND_FOR_GOLD;
import static org.user.service.UserServiceImpl.MIN_LOGCOUNT_FOR_SILVER;
import static org.user.service.UserServiceImpl.MIN_RECCOMEND_FOR_GOLD;

@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = "/org/user/dao/DaoFactory.java")
@ActiveProfiles("test")
@ContextConfiguration(classes = AppContext.class)
public class UserServiceTest {

    @Autowired
    ApplicationContext context; // 팩토리 빈을 가져오려면 애플리케이션 컨텍스트가 필요함.

    @Autowired
    PlatformTransactionManager transacionManager;
    @Autowired
    private UserService UserService;
    @Autowired
    private UserService testUserService;
//    @Autowired
//    private UserServiceImpl userServiceImpl;
    @Autowired
    private UserDaoJdbc userDao;
    @Autowired
    private DataSource dataSource;
    @Autowired
    MailSender mailSender;
    private List<User> users;

    @Test
    public void updateLevels() {
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }
        UserService.upgradeLevels();

//        checkLevel(users.get(0), Level.BASIC);
//        checkLevel(users.get(1), Level.SILVER);
//        checkLevel(users.get(2), Level.SILVER);
//        checkLevel(users.get(3), Level.GOLD);
//        checkLevel(users.get(4), Level.GOLD);
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);
    }

    private void checkLevel(User user, Level expectedLevel) {
        User userUpdate = userDao.get(user.getId());
        assertThat(userUpdate.getLevel(), is(expectedLevel));
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());
        if (upgraded) {
            assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
        } else {
            assertThat(userUpdate.getLevel(), is(user.getLevel()));
        }
    }

    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4); //레벨 이미 지정된 User라면 초기화x
        User userWithoutLevel = users.get(0); //레벨 비어있으면 초기화
        userWithoutLevel.setLevel(null);

        UserService.add(userWithLevel);
        UserService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel(), is(userWithLevelRead.getLevel()));
        assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
    }

    @Before
    public void setUp() {
        DataSource dataSource = new SingleConnectionDataSource(
                "jdbc:mysql://localhost/testdb", "spring", "book", true);
        userDao.setDataSource(dataSource);

        users = Arrays.asList(
                new User("bumin", "박범진", "p1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER - 1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
                new User("erwins", "신승한", "p3", Level.SILVER, MIN_RECCOMEND_FOR_GOLD - 1, 29),
                new User("madnite1", "이상호", "p4", Level.SILVER, MIN_RECCOMEND_FOR_GOLD, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    public static class TestUserService extends UserServiceImpl {
        private String id = " madnite1";
        @Override //트랜잭션 경계설정 테스트 -> get으로 시작하는 메소드를 오버라이드
        public List<User> getAll(){
            for(User user : super.getAll()){
                super.update(user); //강제로 쓰기시도 -> 여기서 읽기 전용 속성으로 인한 예외가 발생해야함.
            }
            return null;
        }
        
        @Override
        protected void upgradeLevel(User user) {
            // 지정된 id의 User 오브젝트가 발견되면 예외를 던져서 작업을 강제로 중단.
            if (user.getId().equals(this.id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }
    }

    @Test(expected = TransientDataAccessException.class) // 일단 어떤 예외가 던져질지 모르므로 처음엔 expecdted 없이 작성
    public void readOnlyTransactionAttribute(){
        testUserService.getAll(); // 트랜잭션 속성이 제대로 적용 됐다면 여기서 읽기전용 속성을 위반했기 때문에 예외 발생해야함.
    }
    
    
    static class TestUserServiceException extends RuntimeException {
    }

    static class MockMailSender implements MailSender {
        private List<String> requests = new ArrayList<String>();

        public List<String> getRequests() {
            return requests;
        }

        public void send(SimpleMailMessage mailMessage) throws MailException {
            requests.add(mailMessage.getTo()[0]); //간단하게 첫 수신자메일만 저장함
        }

        public void send(SimpleMailMessage[] mailMessages) throws MailException {

        }
    }

    static class MockUserDao implements UserDao {
        private List<User> users; // 레벨 업그레이드 후보 User 오브젝트 목록
        private List<User> updated = new ArrayList<>(); // 업그레이드 대상 오브젝트를 저장해둘 목록

        private MockUserDao(List<User> users) {
            this.users = users;
        }

        public List<User> getUpdated() {
            return this.updated;
        }

        public List<User> getAll() { // 스텁기능 제공
            return this.users;
        }

        public void update(User user) { // 목 오브젝트 기능 제공.
            updated.add(user);
        }

        @Override
        public void add(User user) { // 테스트에 사용되지 않는 메소드
            throw new UnsupportedOperationException();
        }

        @Override
        public User get(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
//    @DirtiesContext --> DI를 통해 테스트가 이루어지므로 컨텍스트 수정이 없어져서 불필요!
    public void upgradeAllOrNothing() throws Exception {
//        TestUserService testUserService = new TestUserService(users.get(3).getId()); // 타겟
//        testUserService.setUserDao(this.userDao); // 수동 DI
//        testUserService.setTransactionManager(transacionManager);
//        testUserService.setMailSender(mailSender);

//        UserServiceTx txUserService = new UserServiceTx(); // 직접 프록시 구현
//        txUserService.setTransactionManager(transacionManager);
//        txUserService.setUserService(txUserService);

//        TransactionHandler txHandler = new TransactionHandler(); // 핸들러 (부가기능 구현) //프록시팩토리사용
//        txHandler.setTarget(testUserService); //타겟 설정
//        txHandler.setTransactionManager(transacionManager);
//        txHandler.setPattern("upgradeLevels");
//        UserService txUserService = (UserService) Proxy.newProxyInstance( // 프록시 직접 생성  --> DI로 받을 수도 있음
//                getClass().getClassLoader(),
//                new Class[]{UserService.class},
//                txHandler); //UserService 인터페이스 타입의 다이내믹 프록시 생성

//        TxProxyFactoryBean txProxyFactoryBean = // DI사용하기위해 FactoryBean구현 테스트를 위한 프록시 팩토리 가져오기
//                context.getBean("&userService", TxProxyFactoryBean.class); //팩토리 빈 자체 가져오기
//        txProxyFactoryBean.setTarget(testUserService);// 테스트용 타겟 주입
//        UserService txUserService = (UserService) txProxyFactoryBean.getObject();

//        ProxyFactoryBean txProxyFactoryBean =
//                context.getBean("&userService", ProxyFactoryBean.class);
//        txProxyFactoryBean.setTarget(testUserService);
//        UserService txUserService = (UserService) txProxyFactoryBean.getObject();

        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }
        try {
            this.testUserService.upgradeLevels();
            fail("TestUserServiceException expected"); // 예외 테스트이므로 정상종료라면 실패
        } catch (TestUserServiceException e) {
            //TestUserService가 던져주는 예외를 잡아서 계속 진행되도록 함.
        }
        checkLevelUpgraded(users.get(1), false);  // users.get(1)의 인스턴스는 레벨 업데이트 된 상태
    }

    @Test
    @DirtiesContext // 컨텍스트의 DI 설정을 변경하는 테스트라는 것을 알려줌
    public void upgradedLevels() throws Exception {
        UserServiceImpl userServiceImpl = new UserServiceImpl(); //고립 테스트에서는 직접 생성
        MockUserDao mockUserDao = new MockUserDao(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels();

        List<User> updated = mockUserDao.getUpdated();
        assertThat(updated.size(), is(2));


        checkUserAndLevel(updated.get(0), "joytouch", Level.SILVER);
        checkUserAndLevel(updated.get(1), "madnite1", Level.GOLD);

        List<String> request = mockMailSender.getRequests();
        assertThat(request.size(), is(2));
        assertThat(request.get(0), is(users.get(1).getEmail()));
        assertThat(request.get(1), is(users.get(3).getEmail()));
    }

    @Test
    public void mockUpgradeLevel() throws Exception {
        UserServiceImpl userService = new UserServiceImpl();

        UserDao mockUserDao = mock(UserDao.class);
        when(mockUser.Dao.getAll()).thenReturn(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MailSender mockMailSender = mock(MailSender.class);
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels();

        verify(mockUserDao, times(2)).update(any(User.class));
        verify(mockUserDao, times(2)).update(any(User.class));
        verify(mockUserDao).update(users.get(1));
        assertThat(users.get(1).getLevel(), is(Level.SILVER));
        verify(mockUserDao).update(users.get(3));
        assertThat(users.get(3).getLevel(), is(Level.GOLD));

        ArgumentCaptor<SimpleMailMessgae> mailMessageArg =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mockMailSender, times(2)).send(mailMessageArg.capture());
        List<SimpleMailMessage> mailMessages = mailMessageArg.getAllValues();
        assertThat(mailMessages.get(0).getTo()[0], is(users.get(1).getEmail()));
        assertThat(mailMessages.get(1).getTo()[0], is(users.get(3).getEmail()));


    }

    private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
        assertThat(updated.getId(), is(expectedId));
        assertThat(updated.getLevel(), is(expectedLevel));
    }
}
