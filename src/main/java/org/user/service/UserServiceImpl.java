package org.user.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.user.dao.Level;
import org.user.dao.UserDao;
import org.user.domain.User;

import java.util.List;

@Service("userService")
public class UserServiceImpl implements UserService{
    public static final int MIN_LOGCOUNT_FOR_SILVER = 50;
    public static final int MIN_RECCOMEND_FOR_GOLD = 30;
    UserDao userDao;


    private MailSender mailSender;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }


    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void deleteAll(){
        userDao.deleteAll();
    }
    public User get(String id){
        return userDao.get(id);
    }
    public List<User> getAll(){
        return userDao.getAll();
    }
    public void update(User user){
        userDao.update(user);
    }


//    public void upgradeLevels() { //사용자 레벨 관리 기능
//        List<User> users = userDao.getAll();
//        for (User user : users) {
//            Boolean changed = null;
//            if (user.getLevel() == Level.BASIC && user.getLogin() >= 50) {
//                user.setLevel(Level.SILVER);
//                changed = true;
//            } else if (user.getLevel() == Level.SILVER && user.getRecommend() >= 30) {
//                user.setLevel(Level.GOLD);
//                changed = true;
//            } else if (user.getLevel() == Level.GOLD) {
//                changed = false;
//            } else {
//                changed = false;
//            }
//            if (changed) {
//                userDao.update(user);
//            }
//        }
//    }

    public void add(User user) {
        if (user.getLevel() == null) {
            user.setLevel(Level.BASIC);
        }
        userDao.add(user);
    }

    public void upgradeLevels() throws Exception {
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }

    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC:
                return (user.getLogin() >= MIN_LOGCOUNT_FOR_SILVER);
            case SILVER:
                return (user.getRecommend() >= MIN_RECCOMEND_FOR_GOLD);
            case GOLD:
                return false;
            default:
                throw new IllegalArgumentException("Unkown Level: " + currentLevel);
        }
    }

    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
        sendUpgradeEMail(user);
    }

    private void sendUpgradeEMail(User user) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setFrom("useradmin@ksug.org");
        mailMessage.setSubject("Upgrade 안내");
        mailMessage.setText("사용자님의 등급이" + user.getLevel().name());
        this.mailSender.send(mailMessage);
    }


}
