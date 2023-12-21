# 7장 스프링 핵심 기술의 응용
- 스프링 3대 핵심 기술 IoC/DI, 서비스 추상화, AOP

## SQL과 DAO의 분리
- DB테이블과 필드정보를 담고 있는 SQL 문장 분리하기

### XML 설정을 이용한 분리
- SQL은 문자열로 되어 있으니 설정파일에 프로퍼티 값으로 정의해서 DAO에 주입해줄 수 있음.
#### 개별 SQL 프로퍼티 방식
- UserDaoJdbc 클래스의 SQL 6개를 프로퍼티로 만들고 이를 XML에서 지정하도록 하기
- 이렇게하면 간단히 SQL을 코드에서 분리 가능
- add() 메소드의 SQL을 외부로 뺴는 작업
```java
class ex{
    public String sqlAdd;
    public void setSqlAdd(String sqlAdd){
        this.sqlAdd = sqlAdd;
    }

    public void add(User user) {
//        this.jdbcTemplate.update("insert into users(id, name, password, email, level, login, recommend) " +
//                        "values(?,?,?,?,?,?)",
        this.jdbcTemplate.update(
                this.sqlAdd,
                user.getId(), user.getName(), user.getPassword(), user.getEmail(),
                user.getLevel().intValue(),
                user.getLogin(),
                user.getRecommend());
    }
}
```
- 다음은 XML 설정의 userDao 빈에 다음과 같이 sqlAdd 프로퍼티를 추가하고 SQL을 넣어줌
```xml
<bean id="userDao" class="springbook.user.dao.UserDaoJdbc">
    <property name="dataSource" ref="dataSource" />
    <property name="sqlAdd" value="insert into users(id, name, password, email, level, login, recommend) values(?,?,?,?,?,?,?) "/>
</bean>
```
- 위 방법은 매번 새로운 SQL이 필요할 때마다 프로퍼티를 추가하고 DI를 위한 변수와 수정자 메소드도 만들어줘야함.
#### SQL 맵 프로퍼티 방식
- SQL을 하나의 컬렉션으로 담아두는 방식 -> 맵을 이용하면 키값을 이용해 SQL 문장을 가져올 수 있음
- 맵을 이용하면 프로퍼티는 하나만 만들어도 되기 때문에 DAO 코드는 더 간결해짐
```java
class ex{
    private Map<String, String> sqlMap;

    public void setSqlMap(Map<String, String> sqlMap) {
        this.sqlMap = sqlMap;
    }
    public void add(User user) {
        this.jdbcTemplate.update(
                this.sqlMap.get("add"),
                user.getId(), user.getName(), user.getPassword(), user.getEmail(),
                user.getLevel().intValue(),
                user.getLogin(),
                user.getRecommend());
    }
}
```
- xml 설정 -> Map은 하나 이상의 복잡한 정보를 담고 있기 때문에 \<property\> 태그의 value 애트리뷰트로는 정의해줄 수가 없음
- -> 스프링이 제공하는 \<map\>태그를 사용 -> 스프링은 외에도 다양한 컬렉션 타입 프로퍼티 태그 제공 
```xml
<bean id="userDao" class="springbook.user.dao.UserDaoJdbc">
    <property name="dataSource" ref="dataSource" />
    <property name="sqlMap">
        <map>
            <entry key="add" value="insert into users(id, name, password, email, level, login, recommend) values(?,?,?,?,?,?,?) "/>
        </map>
    </property>
</bean> 
```

### SQL 제공 서비스 
- -> 위 방식처럼 SQL과 DI 설정정보가 섞여 있으면 관리하기 좋지 않음.
- 스프링의 설정파일로부터 생성된 오브젝트와 정보는 애플리케이션을 다시 시작하기 전에는 변경이 매우 어렵다는 점도 문제
- -> 운영중에 동적으로도 갱신 가능한 SQL 서비스 만들기
#### SQL 서비스 인터페이스
- DAO가 사용할 SQL서비스 기능 : SQL에 대한 키 값을 전달하면 해당하는 SQL 돌려주기
```java
public interface SqlService {
    String getSql(String key) throws SqlRetrievalFailureException;
}
```
- SqlRetrievalFailureException의 서브 클래스 만들기 -> 예외의 원인을 구분하기
```java
public class SqlRetrievalFailureException extends RuntimeException{
    public SqlRetrievalFailureException(String message){
        super(message);
    }
    public SqlRetrievalFailureException(String message, Throwable cause){
        super(message, cause);
    }
}
```
```java
class UserDaoJdbc implements UserDao {
    private SqlService sqlService;

    public void setSqlService(SqlService sqlService) {
        this.sqlService = sqlService;
    }
    //..
}
```
```java
class ex{
    // ..
    public void add(User user) {
        this.jdbcTemplate.update(
                this.sqlService.getSql("userAdd"),
                user.getId(), user.getName(), user.getPassword(), user.getEmail(),
                user.getLevel().intValue(),
                user.getLogin(),
                user.getRecommend());
    }
}
```

#### 스프링 설정을 사용하는 단순 SQL 서비스 
- 가장 간단한 방법 
- 키와 SQL을 엔트리로 갖는 맵을 빈 설정에 넣었던 방법 그대로 적용
```java
public class SimpleSqlService implements SqlService {
    private Map<String, String> sqlMap;

    public void setSqlMap(Map<String, String> sqlMap) {
        this.sqlMap = sqlMap;
    }

    public String getSql(String key) throws SqlRetrievalFailureException {
        String sql = sqlMap.get(key);
        if (sql == null) {
            throw new SqlRetrievalFailureException(key + "에 대한 SQL을 찾을 수 없습니다.");
        } else {
            return sql;
        }
    }
}
```
```xml
<beans>
    <bean id="userDao" class="springbook.dao.UserDaoJdbc">
        <property name="dataSource" ref="dataSource"/>
        <property name="sqlService" ref="sqlService"/>
    </bean>
    <bean id="sqlService" class="springbook.user.sqlservice.SimpleSqlService">
        <property name="sqlMap">
            <map>
                <entry key="userAdd" value="insert into users(id, name, password, email, level, login, recommend) values(?,?,?,?,?,?,?)"/>
            </map>
        </property>
    </bean>
</beans>
```
- 이제 sqlService 빈에는 DAO에 영향을 주지 않은 채로 다양한 방법으로 구현된 SqlService 타입 클래스를 적용 가능

## 인터페이스의 분리와 자기참조 빈
- SqlService 인터페이스의 구현방법 고민해보기
- 인터페이스가 있으니 기계적으로 구현 클래스 하나만 만들면 될거라고 생각하면 오산
- 어떤 인터페이스는 **그 뒤에 숨어있는 방대한 서브 시스템의 관문에 불과할 수도 있음!!**
- 인터페이스로 대표되는 기능을 구현 방법과 확장 가능성에 따라 유연한 방법으로 재구성할 수 있도록 설계할 필요도 있음. 

### XML 파일 매핑
- 스프링 설정파일에서 SQL 정보를 넣지말고, SQL을 저장해두는 전용 포맷 파일을 이용하는 것이 바람직.
#### JAXB
- XML에 담긴 정보를 읽어오는 방법은 다양함
- 여기서는 간단하게 사용할 수있는 JAXB(Java Architecture for XML Binding)을 이용
- DOM과 같은 전통적인 XML API와 비교했을 떄 JAXB의 장점은 XML 문서 정보를 거의 동일한 구조의 오브젝트로 직접 매핑해준다는 것.
- -> DOM은 XML 정보를 마치 자바의 리플렉션 API을 이용해서 오브젝트를 조작하는 것처럼 간접적으로 접근 해야하는 불편
- JAXB은 XML의 정보를 그대로 담고 있는 오브젝트 트리 구조로 만들어주기 때문에 XML 정보를 오브젝트처럼 다룰 수 있어 편리함
- -> JAXB는 XML 문서의 구조를 정의한 스키마를 이용해서 매핑할 오브젝트의 클래스까지 자동으로 만들어주는 컴파일러도 제공
- -> 스키마 컴파일러를 통해 자동 생성된 오브젝트에는 매핑 정보가 애노테이션으로 담겨 있음
- -> JAXB API는 애노테이션에 담긴 정보를 이용해서 XML과 매핑된 오브젝트 트리 사이의 자동변환 작업을 수행해줌. 
![](img/img_39.png)

#### SQL 맵을 위한 스키마 작성과 컴파일 
- SQL 정보는 키와 SQL의 목록으로 구성된 맵 구조로 만들면 편리함
```xml
<sqlmap>
    <sql key="userAdd">insert into users(...) ...</sql>
<!--    ... -->
</sqlmap>
```
- xml 문서 구조를 정의하고 있는 xml 스키마
```xml
<element name="sqlmap">
    <complexType>
        <sequence>
            <element name="sql" maxOccurs="unbounded" type="tns:sqlType" />
        </sequence>
    </complexType>
    <complexType name="sqlType">
        <simpleContent>
            <extension base="string">
                <attribute name="key" use="required" type="string" />
            </extension>
        </simpleContent>
    </complexType>
</element>
```
- -> 이렇게 만든 스키마 파일을 sqlmap.xsd라는 이름으로 프로젝트 루트에 저장하고 JAXB 컴파일러로 컴파일하기


#### 언마샬링
- XML문서를 읽어서 자바의 오브젝트로 변환하는 것을 JAXB에서 언마샬링이라고 부름
- 반대로 오브젝트를 XML으로 변환하는 것은 마샬링 
- -> 자바 오브젝트를 바이트 스트림으로 바꾸는 걸 직렬화라고 부르는 것과 비슷함.
#### JAXB 테스트
```xml
<sqlmap>
    <sql key="add">insert</sql>
    <sql key="get">select</sql>
    <sql key="delete">delete</sql>
</sqlmap>
```

```java
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.io.IOException;

public class JaxbTest {
    @Test
    public void readSqlmap() throws JAXBException, IOException {
        String contextPath = Sqlmap.class.getPackage().getName();
        JAXBContext context = JAXBContext.newInstance(contextPath);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(
                getClass().getResourceAsStream("sqlmap.xml"));
        List<SqlType> sqlList = sqlmap.getSql();
        assertThat(sqlList.get(0).getKey(), is("add"));
        assertThat(sqlList.get(0).getValue(), is("insert"));
        assertThat(sqlList.get(1).getKey(), is("get"));
        assertThat(sqlList.get(1).getValue(), is("select"));
        assertThat(sqlList.get(2).getKey(), is("delete"));
        assertThat(sqlList.get(3).getValue(), is("delete"));
    }
}
```

### XML 파일을 이용하는 SQL 서비스
#### SQL 맵 XML 파일
- SQL은 DAO의 로직 일부라고 볼 수 있으므로 DAO와 같은 패키지에 두는 게 좋음

#### XML SQL 서비스
- 특별한 이유가 없는 한 XML 파일은 한 번만 읽도록
- -> XML 파일로부터 읽는 내용은 어딘가에 저장해두고 DAO에서 요청이 올 때 사용해야함.
- -> 생성자에서 JAXB를 이용해 XML로 된 SQL 문서를 읽어들이고, 변환된 Sql 오브젝트를 맵으로 옮겨서 저장해뒀다가,
- -> DAO요청에 따라 SQL을 찾아서 전달하는 방식으로 구현하기

```java
import org.user.sqlservice.SqlRetrievalFailureException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class XmlSqlService implements SqlService {
    private Map<String, String> sqlMap = new HashMap<String, String>();

    public XmlSqlService() {
        String contextPath = Sqlmap.class.getPackage().getName();
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream("sqlmap.xml"); //UserDao와 같은 클래스 패스의 sqlmap.xml 파일을 변환하기
            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);
            for (SqlType sql : sqlmap.getSql()) {
                sqlMap.put(sql.getKey(), sql.getValue());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSql(String key) throws SqlRetrievalFailureException{
        String sql = sqlMap.get(key);
        if(sql == null){
            throw new SqlRetrievalFailureException(key + "를 이용해서 SQL을 찾을 수 없습니다.");
        }else{
            return sql;
        }
    }
}
```
- sqlService 설정 변경
```xml
<bean id="sqlService" class="springbook.user.sqlservice.XmlSqlService">
</bean> 
```

### 빈의 초기화 작업
- XmlSqlService 몇가지 개선점
- 생성자에서 예외 로직 다루는 것은 좋지 않음
- 파일의 위치와 이름이 코드에 고정되어 있는 것은 좋지 않음. -> 외부에서 DI 설정할 수 있게
- 파일이름을 외부에서 지정할 수 있도록 프로퍼티 추가
```java
public class XmlSqlService implements SqlService {
    //..
    private String sqlmapFile;

    public void setSqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }

    public void loadSql() {
        String contextPath = Sqlmap.class.getPackage().getName();
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream(this.sqlmapFile); //UserDao와 같은 클래스 패스의 sqlmap.xml 파일을 변환하기
            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);
            for (SqlType sql : sqlmap.getSql()) {
                sqlMap.put(sql.getKey(), sql.getValue());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
    //..
}
```
- 외부에서 파일 지정, SQL 읽어들이는 초기화 담당 메소드 만들어주었음
- sqlMapFile이라는 프로퍼티는 빈 설정의 \<property\> 태그를 이용해 지정
#### 스프링 빈 후처리기 활용
- 빈 후처리기는 스프링 컨테이너가 빈을 생성 한 뒤에 부가적인 작업 수행할 수 있음
- AOP를 위한 프록시 자동 생성기가 대표적인 빈 후처리기
- 그 중에서 애노테이션을 이용한 빈 설정을 지원해주는 몇가지 빈 후처리기가 있음
- \<context:annotation-config\> 태그를 만들어 설정파일에 넣어주면 빈 설정 기능에 사용할 수 있는
- -> 특별한 애노테이션 기능을 부여해주는 빈 후처리기들이 등록됨
- -> @PostConstruct
#### @PostConstruct
- 스프링은 @PostConstruct 애노테이션을 빈 오브젝트의 초기화 메소드를 지정하는데 사용함
- @PostConstruct를 초기화 작업을 수행할 메소드에 부여해주면
- -> 스프링은 XmlSqlservice 클래스로 등록된 빈의 오브젝트를 생성하고 DI 작업을 마친 뒤
- -> @PostConstruct가 붙은 메소드를 자동으로 실행해줌.
- -> 생성자와는 달리 프로퍼티 까지 모드 준비된 후 실행됨

```java
import org.user.sqlservice.SqlService;

public class XmlSqlService implements SqlService{
    @PostConstruct
    public void loadSql(){}
}
```
- -> sqlmapFile 프로퍼티의 값을 sqlService 빈의 설정에 넣어주기
- sqlmapFile 프로퍼티 값은 XML 파일의 클래스패스로, UserDao 인터페이스의 패키지로부터 상대적으로 지정 가능
```xml
<bean id="sqlService" class="springbook.user.sqlservice.XmlSqlService">
    <property name="sqlmapFile" value="sqlmap.xml"/>
</bean>
```

### 변화를 위한 준비 : 인터페이스 분리
- 현재 XmlSqlService는 특정 포맷의 XML에서 SQL 데이터를 가져오고, 이를 HashMap 타입의 맵 오브젝트에 저장해둠
- -> SQL을 가져오는 방법에 있어서 특정 기술에 고정되있음
- -> XML 대신 다른 포맷의 파일에서 SQL을 읽어 오게 하려면?
#### 책임에 따른 인터페이스 정의
- 독립적으로 변경 가능한 책임 두가지
- 1 SQL정보를 외부의 리소스로부터 읽어오는 것
- 2 SQL을 보관해두고 있다가 필요할 때 제공해주는 것
#### 구조
- SqlService 구현 클래스가 변경 가능한 책임을 가진
- SqlReader와 SqlRegistry 두가지 타입의 오브젝트를 사용하도록 만듬
- SqlRegistry 는 SqlUpdater가 sql을 런타임시에 변경하도록 사용할 수도 있음
- SqlReader에서 SqlRegistry로 전달하는 과정과 형식을 어떻게 할까?
- -> 구현방식이 다양한 두개의 오브젝트 사이에서 복잡한 정보를 전덜하기 위해서는 어떻게?
- -> 두 오브젝트 사이의 정보를 전달하는 것이 전부라면 SqlService가 중간 과정에서 아예 빠지는 방법을 생각해볼 수도 있음
- SqlService가 SqlReader에게 SqlRegistry전략을 제공해주면서
- -> 이를 이용해 SQL정보를 SqlRegistry에 저장하라고 요청하는 편이 나음
```
sqlReader.readSql(sqlRegistry); // SQL을 저장할 대상인 sqlRegistry 오브젝트를 전달
```
```java
interface SqlRegistry{
    void registerSql(String key, String sql);
    String findSql(String key) throws SqlNotFoundException;
}
```
- 이렇게 만들어두면 불필요하게 SqlService코드를 통해 특정 포맷으로 변환한 SQL정보를 주고 받ㅇ르 필요 없이
- SqlReader가 직접 SqlRegistry에 SQL 정보를 등록할 수 있음
- -> 이렇게 하면 SqlReader와 SqlRegistry는 각자의 구현 방식을 독립적으로 유지하면서 꼭 필요한 관계만 가지고 협력해서 일을 할 수 있는 구조가 됨
- SqlReader가 사용할 SqlRegistry오브젝트를 제공해주는건 SqlService의 코드가 담당
- SqlRegistry가 일종의 콜백 오브젝트처럼 사용됨

### 자기참조 빈으로 시작하기
- SqlService의 구현 클래스는 이제 SqlReader와 SqlRegistry 두 개의 프로퍼티를 DI 받을 수 있는 구조로 변경
- XmlSqlService 클래스 하나가 SqlService, SqlReader, SqlRegistry라는 세개의 인터페이스를 구현
- -> 같은 클래스의 코드이지만 책임이 다른 코드는 직접 접근하지 않고 인터페이스를 통해 간접적으로 사용하는 코드로 변경하기
#### 인터페이스를 이용한 분리
- 일단 XmlSqlService는 SqlService만을 구현한 독립적인 클래스라고 생각하기.
- -> SqlReader와 SqlRegistry 두 개의 인터페이스 타입 오브젝트에 의존하는 구조로 만들기
- DI 를 통해 이 두개의 인터페이스를 구현한 오브젝트 주입 받기
```java
public class XmlSqlService implements SqlService {
    private SqlReader sqlReader;
    private SqlRegistry sqlRegistry;

    public void setSqlReader(SqlReader sqlReader) {
        this.sqlReader = sqlReader;
    }

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }
    //..
}
```
- 다음은 XmlSqlService가 Sqlregistry를 구현하도록 만들기

```java
import org.user.sqlservice.SqlRegistry;
import org.user.sqlservice.SqlService;

public class XmlSqlService implements SqlService, SqlRegistry {
    //..
    //SqlRegistry 구현 부분
    //sqlMap은 sqlRegistry구현의 일부가 되므로 외부에서 직접 접근 불가
    private Map<String, String> sqlMap = new HashMap<String, String>();

    public String findSql(String key) throws SqlNotFoundException {
        String sql = sqlMap.get(key);
        if (sql == null) throw new SqlNotFoundException(key +
                "에 대한 SQL을 찾을 수 없습니다.");
        else return sql;
    }
    public void registerSql(String key, String sql) {
        sqlMap.put(key, sql);
    }
    //..
}
```
- XmlSqlService 클래스가 SqlReader를 구현하도록 만들기
- -> xml파일을 어떻게 읽어오는지 SqlReader의 메소드 뒤로 숨기고
- -> 어떻게 저장해줄지 SqlRegistry타입 오브젝트가 알아서 처리하도록 수정하기
- SqlReader를 구현한 코드에서 XmlSqlService내의 다른 변수와 메소드를 직접 참조하거나 사용하면 안됨
- -> 필요한 경우만 적절한 인터페이스를 통해 접₩

```java
public class XmlSqlService implements SqlService, SqlRegistry, SqlReader{

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
}
```
- 마지막으로 SqlServcie 인터페이스 구현을 마무리
- -> @PostConstruct가 달린 빈초기화 메소드와 
- -> SqlService인터페이스에 선언된 메소드인 getFinder()를 sqlReader와 sqlRegistry를 이용하도록 변경하기

```java
public class XmlSqlService implements SqlService, SqlRegistry, SqlReader{
    @PostConstruct
    public void loadSql(){
        this.sqlReader.read(this.sqlRegistry);
    }
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
```
- loadSql()은 XmlSqlService 구현 방법에 따른 메소드
- getSql()은 SqlService 인터페이스의 메소드
- loadSql() 초기화 메소드에서 sqlReader에게 sqlRegisry를 전달하면서 SQL을 읽어서 저장해두도록 요청
- 빈의 초기화를 담당하는 메소드인 loadSql()로 초기화 작업 때 이런 일을 한다는걸 보여주는 코드만 있으면 됨

#### 자기참조 빈 설정
- 같은 클래스 안에 구현된 내용이기는 하지만
- SqlService의 메소드에서 Sql을 읽을 때는 SqlReader인터페이스를 통해
- Sql를 찾을 때는 SqlRegistry인터페이스를 통해 간접적으로 접근하게 했음
- 빈 설정을 통해 실제 DI가 일어나도록 하기 
- 프로퍼티는 자기 자신을 참조할 수 있음. 수정자 메소드로 주입만 가능하면 됨
```xml
<bean id="sqlService" class="springbook.user.sqlservice.XmlSqlService">
    <property name="sqlReader" ref="sqlService" /> <!-- 자기자신 참조 -->
    <property name="sqlRegistry" ref="sqlService" />  <!-- 자기자신 참조 -->
    <property name="sqlmapFile" value="sqlmap.xml" />
</bean>
```
- -> 자기참조빈은 책임과 관심사가 복잡하게 얽혀있어서 확장이 힘들고 변경에 취약한 구조의 클래스를 
- -> 유연한 구조로 만들려고 할 때 처음 시도해볼 수 있는 방법임

### 디폴트 의존관계
- 확장가능한 인터페이스를 정의하고 그에 따라 메소드를 구분해서 DI가능하도록 코드 재구성함
- 다음은 이를 완전히 분리해두고 DI로 조합해서 사용하게 만드는 단계
#### 확장 가능한 기반 클래스
```java
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
```
- SQL을 저장해두고 찾아주는 기능을 담당했던 코드를 SqlRegistry를 구현하는 독립클래스로 분리
```java
public class HashMapSqlRegistry implements SqlRegistry {
    private Map<String, String> sqlMap = new HashMap<>();

    @Override
    public String findSql(String key) throws SqlNotFoundException {
        String sql = sqlMap.get(key);
        if (sql == null) {
            throw new SqlNotFoundException(key + "를 이용해서 SQL을 찾을 수 없습니다");
        } else {
            return sql;
        }
    }

    @Override
    public void registerSql(String key, String sql) {
        sqlMap.put(key, sql);
    }
}
```
- JAXB를 이용해 XML 파일에서 SQL 정보를 읽어오는 코드를 SqlReader인터페이스의 구현클래스로 독립
- 의존관계 독립적인 빈 설정
```xml
<beans>
    <bean id="sqlService" class="springbook.user.sqlservice.BaseSqlService">
        <property name="sqlReader" ref="sqlReader" />
        <property name="sqlRegistry" ref="sqlRegistry" />
    </bean>
    <bean id="sqlReader" class="springbook.user.sqlservice.JaxbXmlSqlReader">
        <property name="sqlmapFile" value="sqlmap.xml" />
    </bean>
    <bean id="sqlRegistry" class="springbook.user.sqlservice.HashMapSqlRegistry">
    </bean>
</beans>
```

#### 디폴트 의존관계 갖는 빈 만들기 
- 특정 의존 오브젝트가 대분의 환경에서 거의 디폴트라고 해도 좋을만큼
- 기본적으로 사용될 가능성이 있다면, 디폴트 의존관계를 갖는 빈을 만드는 것을 고려해볼 필요가 있음
- **디폴트 의존관계란 외부에서 DI 받지 않는 경우 기본적으로 자동 적용되는 의존관계**를 말함

```java
public class DefaultSqlService extends BaseSqlService {
    public DefaultSqlService() {
        // 생성자에서 디폴트 의존 오브젝트를 직접 만들어서 스스로 DI해줌
        setSqlReader(new JaxbXmlSqlReader());
        setSqlRegistry(new HashMapSqlRegistry());
    }
}
```
- DI설정이 없을 경우 디폴트로 적용하고 싶은 의존 오브젝트를 생성자에서 넣어줌
- DI란 클라이언트 외부에서 의존 오브젝트를 주입해주는 것이지만
- 이렇게 자신이 사용할 디폴트 의존 오브젝트를 스스로 DI하는 방법도 있음
- 코드를 통해 의존관계의 오브젝트를주입해주면 특별히 DI가 필요한 상황이 아닌경우 편리하게 사용 가
```xml
<bean id="sqlService" class="springbook.user.sqlservice.DefaultSqlService" />
```
- -> 그러나 테스트 실패
- -> DefaultSqlService 내부에서 생성하는 JaxbXmlSqlReader의 sqlmapFile 프로퍼티가 비어 있기 떄문
- -> sqlmapFile이 없으면 SQL을 읽어올 대상을 알 수 없으므로 예외가 발생함
- JaxbXmlSqlReader를 디폴트 의존 오브젝트로 직접 넣어줄 때는 프로퍼티를 외부에서 직접 지정할 수가 없음
- -> sqlmapFile을 DefaultSqlService의 프로퍼티로 정의하는 방법 있지만 좋은 방법 아님
- -> sqlmapFile의 경우도 JaxbXmlsSqlReader에 의해 기본적으로 사용될 만한 디폴트값을 가질 수 있지 않을까?
- 디폴트 값을 갖는 JaxbXmlSqlReader
```java
public class JaxbXmlSqlReader implements SqlReader {
    //    private String sqlmapFile;
    private static final String DEFAULT_SQLMAP_FILE = "sqlmap.xml";
    private String sqlmapFile = DEFAULT_SQLMAP_FILE;

    public void setSqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }
}
```
- -> DI를 사용한다고 해서 항상 모든 프로퍼티 값을 설정에 넣고 모든 의존 오브젝트를 빈으로 일일이 지정할 필요는 없음
- -> BaseSqlService와 같이 의존 오브젝틀를 DI 해줌으로써 기능의 일부를 자유롭게 확장할 수 있는 기반을 만들어야하지만,
- -> DefaultSqlService처럼 자주 사용되는 의존 오브젝트는 미리 지정한 디폴트 의존 오브젝트를 설정 없이도 사용할 수 있게 만드는 것도 좋은 방법
- DefaultSqlService는 SqlService를 바로 구현한 것이 아니라 BaseSqlService를 상속했다는 점이 중요함
- DefaultSqlService는 BaseSqlService의 sqlReader와 sqlRegistry 프로퍼티를 그대로 갖고 있있고 다른 구현 오브젝트를 빈설정으로 등록 가능
```xml
<bean id="sqlService" class="springbook.user.sqlservice.DefaultSqlService">
    <property name="sqlRegistry" ref="ultraSuperFastSqlRegistry"/>
</bean>
```

### 서비스 추상화 적용
- 