package org.learningtest.jdk.proxy;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.util.PatternMatchUtils;

public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {
    public void setMappedClassName(String mappedClassName) {
        this.setClassFilter(new SimpleClassFilter(mappedClassName));// setClassFilter 클래스필터 오버라이딩
    }

    static class SimpleClassFilter implements ClassFilter {
        String mappedName;
        public SimpleClassFilter(String mappedName){
            this.mappedName = mappedName;
        }
        @Override
        public boolean matches(Class<?> clazz) {
            return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
        }
    }
}
