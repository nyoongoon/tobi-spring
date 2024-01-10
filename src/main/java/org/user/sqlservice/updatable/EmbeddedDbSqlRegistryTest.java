package org.user.sqlservice.updatable;

import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.HSQL;

public class EmbeddedDbSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {
    EmbeddedDatabase db;
    @Override
    protected UpdatableSqlRegistry createUpdatableSqlRegistry() {
        db = new EmbeddedDatabaseBuilder()
                .setType(HSQL).addScript(
                        "classpath:springbook/user/sqlservice/updatable/sqlRegistrySchema.sql")
                .build();
        EmbeddedDbSqlRegistry embeddedDbSqlRegistry = new EmbeddedDbSqlRegistry();
        embeddedDbSqlRegistry.setDataSource(db);

        return embeddedDbSqlRegistry;
    }
    @After
    public void tearDown() {
        db.shutdown();
    }

    @Test
    public void transactionUpdate(){
        checkFind("SQL1", "SQL2", "SQL3"); // 초기상태확인 -> 롤백 후 상태가 처음과 동일하다는 것 비교 목적

        Map<String, String> sqlmap = new HashMap<String, String>();
        sqlmap.put("KEY1", "Modified1");
        sqlmap.put("KEY9999!@#$", "Modified9999"); // 존재하지 않는 키 -> 에러 발생 -> 롤백 여부 확인

        try{
            sqlRegistry.updateSql(sqlmap);
            fail(); // 예외가 발생되지 않으면 테스트 실해
        }catch (SqlUpdateFailureException e){
            checkFind("SQL1", "SQL2", "SQL3");
        }
    }
}
