package org.user.sqlservice;

import org.user.dao.UserDao;

import java.io.InputStream;

public class JaxbXmlSqlReader implements SqlReader{
//    private String sqlmapFile;
    private static final String DEFAULT_SQLMAP_FILE="sqlmap.xml";
    private String sqlmapFile = DEFAULT_SQLMAP_FILE;
    public void setSqlmapFile(String sqlmapFile){
        this.sqlmapFile = sqlmapFile;
    }
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
}
