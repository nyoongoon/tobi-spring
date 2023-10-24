package org.learningtest.jdk;

public class Message {
    String text;

    private Message(String text){ //생성자가 private으로 선언되어 있음 -> 스프링 빈 오브젝트로 생성 불가..
        this.text = text;
    }

    public String getText(){
        return text;
    }

    public static Message newMessage(String text){ //생성자 대신 스태틱 팩토리 메소드를 통해서만 오브젝트 생성 가능
        return new Message(text);
    }
}
