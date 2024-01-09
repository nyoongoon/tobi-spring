package org.user.sqlservice.updatable;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractUpdatableSqlRegistryTest {
    //UpdatableSqlRegistry 인터페이스를 구현한 모든 클래스에 대해 테스트를 만들 때 사용할 수 있는 추상 테스트 클래스
    UpdatableSqlRegistry sqlRegistry;

    @Before
    public void setUp(){
        sqlRegistry = createUpdatableSqlRegistry();
        sqlRegistry.registerSql("KEY1", "SQL1");
        sqlRegistry.registerSql("KEY2", "SQL2");
        sqlRegistry.registerSql("KEY3", "SQL3");
    }

    // 테스트 픽스처를 생성하는 부분만 추상 메소드로 만들고 서브클래스에서 이를 구현하도록 하기
    abstract protected UpdatableSqlRegistry createUpdatableSqlRegistry();


    @Test
    public void find() {
        checkFindResult("SQL1", "SQL2", "SQL3");
    }
    // 반복적으로 검증하는 부분은 별도의 메소드로 분리
    public void checkFindResult(String expected1, String expected2, String expected3) {
        assertThat(sqlRegistry.findSql("KEY1"), is(expected1));
        assertThat(sqlRegistry.findSql("KEY2"), is(expected2));
        assertThat(sqlRegistry.findSql("KEY3"), is(expected3));
    }

    @Test(expected = SqlNotFoundException.class) // 예외상황에 대한 테스트 의식적으로 하기
    public void unknownKey(){
        sqlRegistry.findSql("SQL9999!@#$");
    }

    @Test
    public void updateSingle(){
        sqlRegistry.updateSql("KEY2", "Modified2");
        checkFindResult("SQL1", "Modified2", "SQL3");
    }

    @Test
    public void updateMulti(){
        Map<String, String> sqlmap = new HashMap<>();
        sqlmap.put("KEY1", "Modified1");
        sqlmap.put("KEY3", "Modified3");

        sqlRegistry.updateSql(sqlmap);
        checkFindResult("Modified1", "SQL2", "Modified3");
    }

    @Test(expected = SqlUpdateFailureException.class)
    public void updateWithNotExistingKey(){
        sqlRegistry.updateSql("SQL9999!@#$", "Modified2");
    }
}
