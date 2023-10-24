package org.user.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TransactionHandler implements InvocationHandler {
    private Object target; //부가기능을 제공할 타겟 오브젝트
    private PlatformTransactionManager transactionManager; //트랜잭션 기능을 제공할 트랜잭션 매니저
    private String pattern; // 트랜잭션을 적용할 메소드 이름의 패턴

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().startsWith(pattern)) { // 트랜잭션 적용 대산 메소드 선별
            return invokeInTransaction(method, args);
        } else {
            return method.invoke(target, args);
        }
    }

    private Object invokeInTransaction(Method method, Object[] args) throws Throwable {
        TransactionStatus status =
                this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        try{
            Object ret = method.invoke(target, args);
            this.transactionManager.commit(status);
            return ret;
        }catch (InvocationTargetException e){
            this.transactionManager.rollback(status);
            throw e.getTargetException();
        }
    }
}
