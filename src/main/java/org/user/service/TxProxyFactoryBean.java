package org.user.service;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

public class TxProxyFactoryBean implements FactoryBean<Object> { //생성할 오브젝트 타입을 지정할 수도 있지만 범용적으로 하기 위해 Object로
    Object target;
    PlatformTransactionManager transactionManager; //TransactionHandler를 생성할 때 필요
    String pattern;
    Class<?> serviceInterface; //다이나믹 프록시를 생성할 때 필요. UserService외의 인터페이스를 가진 타깃에도 적용.

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager){
        this.transactionManager = transactionManager;
    }

    public void setPattern(String pattern){
        this.pattern = pattern;
    }

    public void setServiceInterface(Class<?> serviceInterface){
        this.serviceInterface = serviceInterface;
    }

    //FactoryBean 인터페이스 구현 메소드
    @Override
    public Object getObject() throws Exception { //DI 받은 정보를 이용해서 TransactionHandler를 사용하는 다이나믹 프록시를 생성
        TransactionHandler txHanler = new TransactionHandler();
        txHanler.setTarget(target);
        txHanler.setTransactionManager(transactionManager);
        txHanler.setPattern(pattern);
        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{serviceInterface},
                txHanler);
    }

    @Override
    public Class<?> getObjectType() {  //DI 받은 인터페이스 타입에 따라 달라짐
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() { //싱글톤 빈이 아니라는 뜻이 아니라 getObject()가 매번 같은 오브젝트를 리턴하지 않는다는 의미!
        return false;
    }
}
