package org.user.domain;

import org.user.dao.Level;

public class User {
    Level level;
    int login;
    int recommend;

    String id;
    String name;
    String password;

    public User() {
    }

    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public Level getLevel(){
        return level;
    }
    public void setLevel(Level level) {
        this.level = level;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}