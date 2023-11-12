package org.learningtest.jdk.proxy;


import net.sf.cglib.proxy.MethodProxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.learningtest.jdk.Hello;
import org.learningtest.jdk.HelloTarget;
import org.learningtest.jdk.UppercaseHandler;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;


import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.sun.org.apache.xerces.internal.util.PropertyState.is;
import static org.junit.Assert.assertThat;


public class DymanicProxyTest {
    @Test
    public void simpleProxy(){ //JDK 다이나맥 프록시 생성
        Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {Hello.class},
                new UppercaseHandler(new HelloTarget())); // 기존엔 핸들러마다 타겟을 설정해줘야해서 싱글톤 불가했음
    }

    @Test
    public void proxyFactoryBean(){
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget()); // 타깃 설정
        pfBean.addAdvice(new UppercaseAdvice()); // 부가기능을 담은 어드바이스를 추가함. 여러개를 추가할 수도 있음.
        Hello proxiedHello = (Hello) (Hello) pfBean.getObject(); //FactoryBean을 구현했으므로 getObject()로 생성된 프록시 가져옴
        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("THANK YOU TOBY"));
    }

    static class UppercaseAdvice implements MethodInterceptor { //ProxyFactoryBean에 addAdvice()에 넣으면 알아서 타겟의 정보도 전달됨..
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            String ret = (String) invocation.proceed(); // 리플렉션 Method와 달리 메소드 실행시 타겟오브젝트 전달 필요 없음
            return ret.toUpperCase(); //부가기능 적용     // MethodInvocation은 메소드 정보와함께 타겟 오브젝트를 알고 있기 때문..
        }

//        @Override // InvocationHandler와 비교용
//        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//            String ret = (String) method.invoke(target, args); //타겟으로 위임. 인터페이스으 모든 메소드 호출에 적용됨
//            return ret.toUpperCase(); // 부가기능 제공
//        }
    }

    @Test
    public void pointcutAdvisor(){
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());

        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedName("sayH*"); // 이름비교조건설정.

        pfBean.addAdvice(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice())); // 포인트컷과 어드바이스를 Advisor로 묶어서 한 번에 추가
        
        Hello proxiedHello = (Hello) pfBean.getObject();
        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby")); //포인트컷 선정조건에 맞지않음
    }

    @Test
    public void classNamePointcutAdvisor(){
        // 포인트컷 준비
        NameMatchMethodPointcut classMethodPoincut = new NameMatchMethodPointcut(){
            public ClassFilter getClassFilter(){ // 익명 내부 클래스 방식으로 클래스 정의
                return new ClassFilter(){
                    public boolean matches(Class<?> clazz){ // 클래스 필터 포인트컷 재정의
                        return clazz.getSimpleName().startsWith("HelloT");
                    }
                };
            }
        };
        classMethodPoincut.setMappedName("sayH*"); // 메소드 매처 포인트컷
        // 테스트
        checkAdvice(new HelloTarget(), classMethodPoincut, true);

        class HelloWorld extends HelloTarget{};
        checkAdvice(new HelloWorld(), classMethodPoincut, false);

        class HelloToby extends HelloTarget{};
        checkAdvice(new HelloToby(), classMethodPoincut, true);
    }

    @Test
    public void advisorAutoProxyCreator(){
        assertThat(testUserService, is(java.lang.reflect.Proxy.class));
    }


    private void checkAdvice(Object target, Pointcut pointcut, boolean adviced){
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(target);
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
        Hello proxiedHello = (Hello) pfBean.getObject();

        if(adviced){
            assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
            assertThat(proxiedHello.sayHi("Toby"), is ("HI TOBY"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }else{
            assertThat(proxiedHello.sayHello("Toby"), is("Hello Toby"));
            assertThat(proxiedHello.sayHi("Toby"), is("Hi Toby"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }
    }
}
