package org.learningtest.jdk;

public class HelloUppercase implements Hello { // 부가기능 프록시
    Hello hello; // 다른 프록시 추가할 수도 있으므로 인터페이스로 접근

    public HelloUppercase(Hello hello) {
        this.hello = hello;
    }

    @Override
    public String sayHello(String name) {
        return hello.sayHello(name).toUpperCase();
    }

    @Override
    public String sayHi(String name) {
        return hello.sayHi(name).toUpperCase();
    }

    @Override
    public String sayThankYou(String name) {
        return hello.sayThankYou(name).toUpperCase();
    }
}
