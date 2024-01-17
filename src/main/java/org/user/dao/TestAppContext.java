package org.user.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.user.service.DummyMailSender;
import org.user.service.UserService;
import org.user.service.UserServiceTest;

@Configuration
public class TestAppContext {
    @Bean
    public UserService testUserService() {
        return new UserServiceTest.TestUserService();
    }

    @Bean
    public MailSender mailSender() {
        return new DummyMailSender();
    }
}
