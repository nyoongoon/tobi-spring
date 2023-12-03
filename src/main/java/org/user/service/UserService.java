package org.user.service;

import org.user.domain.User;

import java.util.List;

public interface UserService {
    void add(User user); // DAO의 메소드와 1:1대응되는 CRUD메소드이지만 add()(부가기능적용) 처럼 단순 위임이상의 로직을 가질 수 있음.
    User get(String id);
    List<User> getAll();
    void delete(User user);
    void upgradeLevels();
}
