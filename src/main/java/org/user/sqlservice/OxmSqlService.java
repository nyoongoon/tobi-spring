package org.user.sqlservice;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.user.dao.UserDao;

import javax.annotation.PostConstruct;

import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

public class OxmSqlService implements SqlService {
    // SqlService의 실제 구현 위임할 대상인 BaseSqlService를 인스턴스 변수로 정의
    private final BaseSqlService baseSqlService = new BaseSqlService();

    //final이므로 변경 불가능. -> OxmSqlService와 OxmSqlReader는 강하게 결합되서 하나의 빈으로 등록되고 한 번에 설정 할 수 있음
    private final OxmSqlReader oxmSqlReader = new OxmSqlReader();

    private SqlRegistry sqlRegistry = new HashMapSqlRegistry();

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.oxmSqlReader.setUnmarshaller(unmarshaller);
    }

    // 리소스 추상화 이전
//    public void setSqlmapFile(String sqlmapFile) {
//        this.oxmSqlReader.setSqlmapFile(sqlmapFile);
//    }

    public void setSqlmap(Resource sqlmap){
        this.oxmSqlReader.setSqlmap(sqlmap);
    }

    @PostConstruct
    public void loadSql(){
        // OxmSqlService의 프로퍼티를 통해서 초기화된 SqlReader와 SqlRegistry를 실제 작업을 위임할 대상인 baseSqlService에 주입
        this.baseSqlService.setSqlReader(this.oxmSqlReader);
        this.baseSqlService.setSqlRegistry(this.sqlRegistry);
        this.baseSqlService.loadSql(); //SQL을 등록하는 초기화 작업을 baseSqlService에 위임
    }

    public String getSql(String key) throws SqlRetrievalFailureException{
        return this.baseSqlService.getSql(key);
    }

//    BaseSqlService로 위임하기
//    @PostConstruct
//    public void loadSql() {
//        this.oxmSqlReader.read(this.sqlRegistry);
//    }
//
//    public String getSql(String key) throws SqlRetrievalFailureException {
//        try {
//            return this.sqlRegistry.findSql(key);
//        } catch (SqlNotFoundException e) {
//            throw new SqlRetrievalFailureException(e);
//        }
//    }


    //private 멤버 클래스로 정의. 톱레벡 클래스인 OxmSqlService만 사용가능함.
    private class OxmSqlReader implements SqlReader {
        private Unmarshaller unmarshaller;
        private final static String DEFAULT_SQLMAP_FILE = "sqlmap.xml";

        //디폴트 파일은 기존과 같지만 이제는 Resource 구현클래스인 ClassPathResource를 이용한다.
        private Resource sqlmap = new ClassPathResource("sqlmap.xml", UserDao.class);
//        private String sqlmapFile = DEFAULT_SQLMAP_FILE;
        public void setUnmarshaller(Unmarshaller unmarshaller) {
            this.unmarshaller = unmarshaller;
        }

        public void setSqlmap(Resource sqlmap){ //주입해주기 원할 때 사용
            this.sqlmap = sqlmap;
        }
//        public void setSqlmapFile(String sqlmapFile) { //디폴트 사용하지 않을 경우 사용
//            this.sqlmapFile = sqlmapFile;
//        }
        

        public void read(SqlRegistry sqlRegistry) {
            try {
//                Source source = new StreamSource(
//                        UserDao.class.getResourceAsStream(this.sqlmapFile));
                Source source = new StreamSource(sqlmap.getInputStream()); //Resource 사용
                Sqlmap sqlmap = (Sqlmap) this.unmarshaller.unmarshal(source);
                for(SqlType sql : sql.getSql()){
                    sqlRegistry.registerSql(sql.getKey(), sql.getValue());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(this.sqlmap.getFilename() + "을 가져올 수 없습니다.", e);
            }
        }
    }
}
