package org.learningtest.jdk;

import org.springframework.beans.factory.FactoryBean;

public class MessageFactoryBean implements FactoryBean<Message> {
    String text;
    public void setText(String text) { // 오브젝트 생성할 때 필요한 정보를 팩토리 빈의 프로퍼티로 설정해서 대신 DI 받을 수 있게 함.
        this.text = text;              // 주입된 정보는 오브젝트 생성 중에 사용된다..
    }
    @Override
    public Message getObject() throws Exception { // 실제 빈으로 사용될 오브젝트를 직접 생성한다.
        return Message.newMessage(this.text);     // 코드를 이용하기 때문에 복잡한 방식의 오브젝트 생성과 초기화 작업도 가능
    }
    @Override
    public Class<?> getObjectType() {
        return null;
    }
    @Override
    public boolean isSingleton() {  // getObject() 메소드가 돌려주는 오브젝트가 싱글톤이지 알려줌
        return false;               // 이 팩토리 빈은 매번 요청할 때마다 새로운 오브젝트를 만들므로 false로 설정
    }                               // -> 팩토리 빈의 동작방식에 관한 설정. 만들어진 빈 오브젝트는 싱글톤으로 스프링이 관리해줄 수 있음..
}
