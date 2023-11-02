package org.user.service;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TransactionAdvice implements MethodInterceptor {
    PlatformTransactionManager transactionManager;

    public void setTransactionManager(PlaformTransactionManager transactionManager){
        this.transactionManager = transactionManager;
    }

    // 타깃을 호출하는 기능을 가진 콜백오브젝트(MethodInvocation)을 프록시로부터 받음
    // 덕분에 어드바이스는 특정 타깃에 의존하지 않고 재사용 가능 -> 빈 등록 가능
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TransactionStatus status =
                this.transactionManager.getTransaction(
                        new DefaultTransactionDefinition());
        try{
            Object ret = invocation.proceed(); //콜백이용하여 타깃호출
            this.transactionManager.commit(status);
            return ret;
        } catch (RuntimeException e){
            this.transactionManager.rollback(status);
            throw e;
        }
    }
}
