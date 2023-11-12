package org.learningtest.pointcut;

public class Target implements TargetInterface{
    @Override
    public void hello() {

    }

    @Override
    public void hello(String a) {

    }

    @Override
    public void minus(int a, int b) throws RuntimeException {

    }

    @Override
    public int plus(int a, int b) {
        return 0;
    }

    public void method(){}
}
