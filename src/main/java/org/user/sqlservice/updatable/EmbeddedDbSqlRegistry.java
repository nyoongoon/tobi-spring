package org.user.sqlservice.updatable;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Map;

public class EmbeddedDbSqlRegistry implements UpdatableSqlRegistry {
    SimpleJdbcTemplate jdbc;
    TransactionTemplate transactionTemplate; //JdbcTemplate과 트랜잭션을 동기화해주는 트랜잭션 템플릿. 멀티스레드 환경에서 공유 가능

    public void setDataSource(DataSource dataSource) {
        jdbc = new SimpleJdbcTemplate(dataSource);
        //transactionTemplate 추가
        //dataSource로 TransactionManager를 만들고 이를 이용해 TransactionTemplate을 생성하기
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public void registerSql(String key, String sql) {
        jdbc.update("insert into sqlmap(key_, sql_) values(?,?)", key, sql);
    }

    @Override
    public String findSql(String key) throws SqlNotFoundException {
        try {
            return jdbc.queryForObject("select sql_ from sqlmap where key_ = ?",
                    String.class, key);
        } catch (EmptyResultDataAccessException e) {
            throw new SqlNotFoundException(key + "에 해당하는 SQL을 찾을 수 없습니다.", e);
        }
    }

    @Override
    public void updateSql(String key, String sql) throws SqlUpdateFailureException {
        // update는 실행 결과로 영향을 받은 레코드의 개수를 리턴함
        // 이를 이용하면 주어진 키를 가진 SQL이 존재했는지를 간단히 확인 가능
        int affected = jdbc.update("update sqlmap set sql_ = ? where key_ = ?", sql, key);
        if(affected == 0){
            throw new SqlUpdateFailureException(key + "에 해당하는 SQL을 찾을 수 없습니다.");
        }
    }

    @Override
    public void updateSql(final Map<String, String> sqlmap) throws SqlUpdateFailureException {
        // sqlmap은 익명 내부 클래스로 만들어지는 콜백 오브젝트 안에서 사용되는 것이라 final로 선언해줘야함.
        // 트랜잭션 템플릿이 만드는 트랜잭션 경계 안에서 동작할 코드를 콜백형태로 만들고 TransactionTemplate의 execute()에 전달
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for(Map.Entry<String, String> entry : sqlmap.entrySet()){
                    updateSql(entry.getKey(), entry.getValue());
                }
            }
        });
    }
}
