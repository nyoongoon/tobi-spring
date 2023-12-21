package org.user.sqlservice;

import org.user.dao.UserDao;
import org.user.sqlservice.SqlRetrievalFailureException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class XmlSqlService implements SqlService, SqlRegistry, SqlReader {
    private SqlReader sqlReader;
    private SqlRegistry sqlRegistry;

    public void setSqlReader(SqlReader sqlReader){
        this.sqlReader = sqlReader;
    }
    public void setSqlRegistry(SqlRegistry sqlRegistry){
        this.sqlRegistry = sqlRegistry;
    }

    //SqlRegistry 구현 부분
    //sqlMap은 sqlRegistry구현의 일부가 되므로 외부에서 직접 접근 불가
    private Map<String, String> sqlMap = new HashMap<String, String>();
    public String findSql(String key) throws SqlNotFoundException{
        String sql = sqlMap.get(key);
        if(sql ==  null) throw new SqlNotFoundException(key +
                "에 대한 SQL을 찾을 수 없습니다.");
        else return sql;
    }
    public void registerSql(String key, String sql){
        sqlMap.put(key, sql);
    }


    //sqlMapFile은 SqlReader구현의 일부가 되므로 SqlReader구현 메소드를 통하지 않고 접근하면 안됨
    private String sqlmapFile;
    public void setSqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }
    // loadSql()에 있던 코드를 SqlReader 메소드로 가져옴
    // 초기화를 위해 무엇을 할 것인가와 SQL을 어떻게 읽는지를 분리
    public void read(SqlRegistry sqlRegistry){
        String contextPath = Sqlmap.class.getPackage().getName();
        try{
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream(sqlmapFile);
            Sqlmap sqlmap = (Sqlmap)unmarshaller.unmarshal(is);
            for(SqlType sql : sqlmap.getSql()){
                //파라미터로 전달받은 구현코드는 사실 자기 자신이긴하지만
                //다른 오브젝트라고 생각하고 인터페이스에 정의된 메소드를 통해서만 사용해야함.
                sqlRegistry.registerSql(sql.getKey(), sql.getValue());
            }
        }catch (JAXBException e){
            throw new RuntimeException(e);
        }
    }

    //XmlSqlService구현 방법에 따른 메소드
    @PostConstruct
    public void loadSql(){
        this.sqlReader.read(this.sqlRegistry);
    }
    //SqlSerivce인터페이스 메소드
    public String getSql(String key) throws SqlRetrievalFailureException{
        try{
            return this.sqlRegistry.findSql(key);
        }catch (SqlNotFoundException e){
            throw new SqlRetrievalFailureException(e);
        }
    }

//    @PostConstruct
//    public void loadSql(){
//        String contextPath = Sqlmap.class.getPackage().getName();
//        try {
//            JAXBContext context = JAXBContext.newInstance(contextPath);
//            Unmarshaller unmarshaller = context.createUnmarshaller();
//            InputStream is = UserDao.class.getResourceAsStream(this.sqlmapFile); //UserDao와 같은 클래스 패스의 sqlmap.xml 파일을 변환하기
//            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);
//            for (SqlType sql : sqlmap.getSql()) {
//                sqlMap.put(sql.getKey(), sql.getValue());
//            }
//        } catch (JAXBException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public String getSql(String key) throws SqlRetrievalFailureException {
//        String sql = sqlMap.get(key);
//        if (sql == null) {
//            throw new SqlRetrievalFailureException(key + "를 이용해서 SQL을 찾을 수 없습니다.");
//        } else {
//            return sql;
//        }
//    }


}