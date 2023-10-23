package org.user.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TransactionHandler implements InvocationHandler {
    private Object target; //부가기능을 제공할 타겟 오브젝트
    private PlatformTransactionManager transactionManager; //트랜잭션 기능을 제공할 트랜잭션 매니저
    private String pattern; // 트랜잭션을 적용할 메소드 이름의 패턴

    public void setTarget(Object target){
        this.target = target;
    }
    public void setTransactionManager(PlatformTransactionManager transactionManager){
        this.transactionManager = transactionManager;
    }
    public void setPattern(String pattern){
        this.pattern = pattern;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
