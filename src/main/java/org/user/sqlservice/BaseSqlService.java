package org.user.sqlservice;

import org.springframework.transaction.annotation.Transactional;

public class BaseSqlService implements SqlService{
    // BaseSqlService는 상속을 통해 확장해서 사용하기에 적합함
    // 서브클래스 필요한경우 접근할 수 있도록 protected로 선언
    protected SqlReader sqlReader;
    private SqlRegistry sqlRegistry;

    public void setSqlReader(SqlReader sqlReader) {
        this.sqlReader = sqlReader;
    }

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }

    @PostConstruct
    public void loadSql(){
        this.sqlReader.read(this.sqlRegistry);
    }

    @Override
    public String getSql(String key) throws SqlRetrievalFailureException {
        return null;
    }
}
