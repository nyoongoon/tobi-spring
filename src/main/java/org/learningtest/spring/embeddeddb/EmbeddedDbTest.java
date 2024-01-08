package org.learningtest.spring.embeddeddb;

import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class EmbeddedDbTest {
    EmbeddedDatabase db;
    SimpleJdbcTemplate template; // JdbcTemplate을 더 편리하게 사용할 수 있게 확장한 템플릿.
    
    @Before
    public void setUp(){
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .addScript("classpath:/springbook/learningtest/spring/embeddeddb/schema.sql")
                .addScript("classpath:/springbook/learningtest/spring/embeddeddb/data.sql")
                .build();
        //EmbeddedDatabase는 DataSource의 서브 인터페이스이므로 DataSource를 필요로 하는 SimpleJdbcTemplate을 만들 때 사용할 수 있다
        template = new SimpleJdbcTemplate(db); 
    }
    
    @After
    public void tearDown(){ // 매 테스트를 딘행한 뒤에 db를 종료 // 내장형 메모리 db는 따로 저장하지 않는 한 애플리케이션과 같은 생명주기를 갖음
        db.shutdown();
    }
    
    
}
