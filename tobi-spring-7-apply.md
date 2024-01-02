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

## 서비스 추상화 적용
- JaxbXmlSqlReader 개선하기
- 1 JAXB 외 다양한 xml과 자바 오브젝트 매핑 기술로 손쉽게 바꿔서 사용할 수 있어야함
- 2 XML파일을 좀 더 다양한 소스에서 가져올 수 있게 만들기

### OXM 서비스 추상화
- 스프링이 제공하는 OXM 추상화 서비스 인터페이스에는 자바 오브젝트를 XML로 변환하는 Marshaller와 반대로 XML을 자바 오브젝트로 변환하는 Unmarshaller가 있음

```java
package org.springframework.oxm;

import javax.xml.transform.Source;
import java.io.IOException;

public interface Unmarshaller {
    boolean supports(Class<?> clazz);

    Object unmarshal(Source source) throws IOException, XmlMappingException;
}
```
- JAXB 언마살러 등록한 빈설정
```xml
<bean id="unmarshaller" class="org.springframework.oxm.jaxb.Jaxb2Marshaller">
    <property name="contextPath" value="springbook.user.sqlservice.jaxb"/>
</bean>
```
- Castor에서 사용할 매필 정보 담을 xml 따로 생성하기
- unmarshaller 빈의 클래스를 Castor용 구현 클래스로 변경하기 -> mappingLocation 프로퍼티에 준비된 Caster용 매핑 파일의 위치를 지정해주기
```xml
<bean id="unmarshaller"
    class="org.springframework.oxm.caster.CastorMarshaller">
    <property name="mappingLocation"
              value="springbook/learningtest/spring/oxm/mapping.xml"/>
</bean>
```

### OXM 서비스 추상화 적용 
- 스프링의 OXM 추상화 기능을 이용하는 SqlService 만들기
- OxmSqlService라고 하고 SqlRegistry는 DI 받을 수 있게 하지만 SqlReader는 스프링 OXM언마샬러를 이용하도록 OxmSqlService내 고정하기
- -> SQL을 읽는 방식을 OXM으로 제한해서 사용성을 극대화하는게 목적
- -> SQL을 가져오는 방법이 스프링의 OXM 추상화 방식으로 고정된다면 OxmSqlService 클래스 내에 OXM 코드를 넣어도 될까?
- -> 그럴 수 있지만 OxmSqlService가 OXM 기술에 의존적이라고 꼭 OXM 코드를 직접 갖고 있을 필요는 없음
- -> 이미 SqlReader와 SqlRegistry라는 두 개의 전략을 활용하는 구조를 적용해봤으므로
- -> 이를 유지하되 SqlReader 구현 오브젝트에 대한 의존관계를 고정시켜버리는 방법을 생각해볼 수 있음. 
- -> 구현 클래스를 OxmSqlService가 내장하게 만들기..

#### 멤버 클래스를 참조하는 통합 클래스
- OxmSqlService는 BaseSqlService와 유사하게 SqlReader 타입의 의존 오브젝트를 사용하되
- -> 이를 **스태틱 멤버 클래스로 내장하고 자신만이 사용**할 수 있도록 만들기. 
- --> **의존 오브젝트를 자신만이 사용하도록 독점하는 구조**로 만드는 방법
- -> 내장된 SqlReader 구현을 외부에서 사용하지 못하도록 제한하고 스스로 최적화된 구조로 만들어두기
- -> **밖에서 볼 땐 하나의 오브젝트로 보이지만 내부에서는 의존관계를 가진 두개의 오브젝트가 깔끔하게 결합**되서 사용됨
- -> 유연성을 조금 손해보더라도 내부적으로 낮은 결합도를 유지한 채로 응집도가 높은 구현 만들 때 유용
![](img/img_40.png)
- -> SqlRader를 SqlService 클래스 안에 포함시켜만들기 -> 하나의 빈으로 등록 
- 언마샬러 빈은 스프링이 제공해주니 구현필요x
- SqlRegistry는 일단 가장 단순한 HashMapSqlRegistry를 디폴트 의존 오브젝트로 등록 -> 필요하면 DI로 교체
- OxmSqlService와 OxmSqlReader는 **구조적으로 강하게 결합되어 있지만 논리적으로 명확하게 분리되는 구조**
- -> **자바의 스태틱 멤버 클래스를 이런 용도로 쓰기 적합**함.

```java
import org.user.sqlservice.SqlReader;

public class OxmSqlService implements SqlService {
    //final이므로 변경 불가능. -> OxmSqlService와 OxmSqlReader는 강하게 결합되서 하나의 빈으로 등록되고 한 번에 설정 할 수 있음
    private final OxmSqlReader oxmSqlReader = new OxmSqlReader();

    private class OxmSqlReader implements SqlReader{ //private 멤버 클래스로 정의. 톱레벡 클래스인 OxmSqlService만 사용가능함.
        //..
    }
    //..
}
```
- OxmSqlReader는 private 멤버 클래스이므로 외부에서 접근하거나 사용할 수 없음
- 또한 OxmSqlService는 이를 final로 선언하고 직접 오브젝트를 생성하기 때문에 OxmSqlReader를 DI하거나 변경할 수 없음
- -> 이렇게 두 개의 클래스를 강하게 결합하고 더 이상의 확장이나 변경을 제한해두는 이유는 무엇?
- -> OXM을 이용하는 서비스 구조로 최적화하기 위해서 -> 하나의 클래스로 만들어두기 때문에 빈의 등록과 설정은 단순해지고 쉽게 사용가능.
- -> 디폴트 의존 오브젝트 방식은 디폴트 오브젝트 내부에서 값은 주입받기가 힘들다는 점이 문제
- -> OXM을 적용하는 경우는 언마샬러를 비롯해서 DI 설정해줄게 많아지기 때문에 SqlReader 클래스를 단순 디폴트 오브젝트 방식으로 제공할 수 없음 
- -> 이런 경우에는 하나의 빈 설정만으로 SqlService와 SqlReader의 필요한 프로퍼티 설정을 모두 가능하도록 만들 필요가 있음
- --> SqlService의 구현이 SqlReader의 구체적인 구현 클래스가 무엇인지도 알고, 자신의 프로퍼티를 통해 필요한 설정정보도 넘겨주고, 심지어 멤버 클래스로 소유도 하고 있는 강한 결합 구조를 만드는 방법을 사용하는 것임
 
![](img/img_41.png)
- 위는 하나의 빈 설정으로  두개의 오브젝트를 설정하는 구조를 보여줌
- -> OxmSqlService로 등록한 빈의 프로퍼티 일부는 OxmSqlService 내부의 OxmSqlReader 프로퍼티를 설정해주기 위한 창구역할을 함.
- OxmSqlReader는 외부에 노출되지 않기 때문에 OxmSqlService에 의해서만 만들어지고 스스로 빈으로 등록될 수 없음
- -> 자신이 DI통해 제공받아야하는 프로퍼티가 있으면 이를 OxmSqlService의 공개된 프로퍼티를 통해 간접적으로 DI 받아야함!
```java
public class OxmSqlService implements SqlService {
    //final이므로 변경 불가능. -> OxmSqlService와 OxmSqlReader는 강하게 결합되서 하나의 빈으로 등록되고 한 번에 설정 할 수 있음
    private final OxmSqlReader oxmSqlReader = new OxmSqReader();
    //..
    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.oxmSqlReader.setUnmarshaller(unmarshaller);
    }

    public void setSqlmapFile(String sqlmapFile) {
        this.oxmSqlReader.setSqlmapFile(sqlmapFile);
    }
    //private 멤버 클래스로 정의. 톱레벡 클래스인 OxmSqlService만 사용가능함.
    private class OxmSqlReader implements SqlReader {
        private Unmarshaller unmarshaller;
        private String sqlmapFile;
        //setter 메소드 생략
    }
    //..
}
```
- 위의 방식은 UserDaoJdbc안에서 JdbcTemplate을 직접 만들어서 사용한 것과 비슷
- -> UserDaoJdbc는 스스로 DataSource 프로퍼티가 필요하지 않지만, 자신의 프로퍼티로 DataSource 등록해두고,
- -> 이를 DI 받아서 JdbcTemplate을 생성하면서 전달해줌
```
public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
}
```
- 차이점 : JdbcTemplate은 그 자체로 독립된 빈으로 만들수도 있고, 여러 DAO 사용가능
- -> OxmSqlReader는 OxmSqlService에서만 사용하도록 제한한 멤버 클래스라는 점에서 차이가 있음
```java
public class OxmSqlService implements SqlService {
    //final이므로 변경 불가능. -> OxmSqlService와 OxmSqlReader는 강하게 결합되서 하나의 빈으로 등록되고 한 번에 설정 할 수 있음
    private final OxmSqlReader oxmSqlReader = new OxmSqReader();

    private SqlRegistry registry = new HashMapSqlRegistry();

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.oxmSqlReader.setUnmarshaller(unmarshaller);
    }

    public void setSqlmapFile(String sqlmapFile) {
        this.oxmSqlReader.setSqlmapFile(sqlmapFile);
    }

    @PostConstruct
    public void loadSql() {
        this.oxmSqlReader.read(this.sqlRegistry);
    }

    public String getSql(String key) throws SqlRetrievalFailureException {
        try {
            return this.sqlRegistry.findSql(key);
        } catch (SqlNotFoundException e) {
            throw new SqlRetrievalFailureException(e);
        }
    }


    //private 멤버 클래스로 정의. 톱레벡 클래스인 OxmSqlService만 사용가능함.
    private class OxmSqlReader implements SqlReader {
        private Unmarshaller unmarshaller;
        private final static String DEFAULT_SQLMAP_FILE = "sqlmap.xml";
        private String sqlmapFile = DEFAULT_SQLMAP_FILE;

        public void setUnmarshaller(Unmarshaller unmarshaller) {
            this.unmarshaller = unmarshaller;
        }

        public void setSqlmapFile(String sqlmapFile) { //디폴트 사용하지 않을 경우 사용
            this.sqlmapFile = sqlmapFile;
        }

        public void read(SqlRegistry sqlRegistry) {
            try {
                Source source = new StreamSource(
                        UserDao.class.getResourceAsStream(this.sqlmapFile));
                Sqlmap sqlmap = (Sqlmap) this.unmarshaller.unmarshal(source);
                for(SqlType sql : sql.getSql()){
                    sqlRegistry.registerSql(sql.getKey(), sql.getValue());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(this.sqlmapFile + "을 가져올 수 없습니다.", e);
            }
        }
    }
}
```
```xml
<beans>
    <bean id="sqlService" class="springbook.user.sqlservice.OxmSqlService">
        <property name="unmarshaller" ref="unmarshaller" />
    </bean>
    <bean id="unmarshaller" class="org.springframework.oxm.jaxb.Jaxb2Marshaller">
        <property name="contextPath" value="springbook.user.sqlservice.jaxb"/>
    </bean>
</beans>
```

#### 위임을 이용한 BaseSqlService의 재사용
- 위의 OxmSqlService는 SqlReader는 스태틱 멤버 클래스로 고정시켜서 OXM에 특화된 형태로 재구성했기 때문에 
- -> 설정은 간결해지고 의도되지 않는 방식으로 확장될 위험이 없ㅇ므
- 꺼림칙한 부분 : loadSql()과 sqlSql()이라는 SqlService의 핵심 메소드 구현 코드가 BaseSqlService와 동일하다는 점
- -> 프로퍼티 설정을 통한 초기화 작업을 제외하면 두 가지 작업의 코드는 BaseSqlService와 OxmSqlService 양쪽에 중복됨
- -> BaseSqlService 코드를 재사용한다고 이를 상속해서 OxmSqlService를 만들면 멤버 클래스로 통합시킨
- -> OxmSqlReader를 생성하는 코드를 넣기가 애매함
- -> 중복을 제거하기 위해 loadSql()과 getSql() 메소드를 추출해서 슈퍼클래스로 분리할 수도 있겠으나 이정도 코드로 복잡한 계층구조 만들기 부담스러움
- -> 이런 경우는 간단한 코드 중복쯤은 허용하고 BaseSqlService와 독립적으로 OxmSqlService를 관리해나가도 문제는 없을 것..
- 그런데 loadSql()과 getSql()의 작업이 복잡한 경우는? 코드 양이 많고 변경도 자주 일어난다면? 
- -> 중복된 코드를 제거할 방법 -> **위임 구조를 이용해 코드의 중복을 제거**
- -> loadSql()과 getSql()의 구현 로직은 BaseSqlService에만 두고, 
- -> **OxmSqlService는 일종의 설정과 기본 구성을 변경해주기 위한 어댑터 같은 개념**으로 BaseSqlService 앞에 두는 설계가 가능함
- -> OxmSqlService의 외형적인 틀은 유지한채로 SqlService 기능 구현은 BaseSqlService로 위임하는 것.
#### 위임 구조
- 프록시 만들 때 위임구조 사용해 봄
- 위임을 위해서는 두개의 빈을 등록하고 **요청을 직접 받는 빈이 중요 내용을 뒤의 빈에 전달해주는 구조**로 만들어야함
- -> 하지만 OxmSqlService와 BaseSqlService를 위임 구조로 만들기 위해 두 개의 빈으로 등록하는 것은 불편한 일임.
- -> 부가기능 프록시처럼 많은 타깃에 적용할 것도 아니고 ,특화된 서비스를 위해 한 번만 사용할 것이므로 유연한 DI는 포기
- -> OxmSqlService와 BaseSqlService를 한 클래스로 묶는 방법을 생각해보기
![](img/img_42.png)
- -> 의존관계가 복잡해보일 수 있지만 OxmSqlService 자체는 OXM에 최적화된 빈 클래스를 만들기 위한 틀이라고 생각하면 이해하기 쉬움
- OxmSqlService는 OXM 기술에 특화된 SqlReader를 멤버로 내장하고 있고, 그에 필요한 설정을 한 번에 지정할 수 있는 확장구조만을 갖고 있음.
- 실제 SqlReader아 SqlService를 이용해 SqlService의 기능을 구현하는 일은 내부에 BaseSqlService를 만들어서 위임하기.
```java
public class OxmSqlService implements SqlService {
    // SqlService의 실제 구현 위임할 대상인 BaseSqlService를 인스턴스 변수로 정의
    private final BaseSqlService baseSqlService = new BaseSqlService();
    //..
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
    //..
}
```
- 위임 구조를 이용하여 OxmSqlService에 있던 중복 코드를 제거


### 리소스 추상화 
- 지금까지 만든 OxmSqlReader나 XmlSqlReader에는 공통적인 문제점
- -> SQL 매핑 정보가 담긴 XML 파일 이름을 프로퍼티로 외부에서 지정할 수 있지만 UserDao클래스와 같은 클래스패스에 존재하는 파일로 제한됨
- 기존 OxmSqlReader는 클래스패스로부터 리소스 가져오기 위해 ClassLoader 클래스의 getResourceAsStream()을 사용함. 
- -> 파일 시스템이나 웹상의 http를 통해 접근 가능한 파일로 바꾸려면 url클래스를 사용하도록 코드를 변경해야함
- -> 또한 서블릿 컨텍스트 내의 리소스를 가져오려면 ServletContext의 getResrouceAsStream()을 사용해야함. 
- -> 사실 리소스를 가져오면 최종적으로 InputStream 형태로 변환해서 사용해야겠지만, 리소스의 위치와 종류에 따라서 다른 클래스와 메소드를 사용해야한다는 점이 불편함.
- -> 이것도 목적은 동일하지만 사용법이 각기 다른 여러 기술이 존재하는 것으로 생각할 수 있음
- -> 여러가지 종류의 리소스를 어떻게 단일 인터페이스와 메소드로 추상화할지 고민.

#### 리소스
- 스프링은 자바에 존재하는 일관성 없는 리소스 접근 API를 추상화해서 Resource라는 추상화 인터페이스를 정의함.

```java
package org.springframework.core.io;

import java.io.IOException;

public interface Resource extends InputStreamSource {
    // 리소스의 존재나 읽기 가능 여부를 확인할 수 있음. 현재 리소스에 대한 입력 스트림이 열려있는지도 확인 가능
    boolean exists();
    booean isReadable();
    boolean isOpen();

    // JDK의 URL, URI, File 형태로 전한 가능한 리소스에 사용됨. 
    URL getURL() throws IOException;
    URI getURI() throws IOException;
    File getFile() throws IOException;
    Resource createRelative(String relatvicePath) throws IOException;

    // 리소스에 대한 이름과 부가적인 정보를 제공함.
    long lastModified() throws IOException;
    String getFilename();
    String getDescription();
}

public interface InputStreamSource { // 모든 리소스는 InputStream 형태로 가져올 수 있음.
    InputStream getInputStream() throws IOException;
}
```
- 애플리케이션 컨텍스트가 사용할 설정 정보 파일을 지정하는 것부터 시작해서
- **스프링의 거의 모든 API는 외부의 리소스 정보가 필요할 때 항상 이 Resource 추상화를 이용**함.
- -> 어떻게 임의의 리소스를 Resource 인터페이스 타입의 오브젝트로 가져올 수 있을까? 
- -> 다른 서비스 추상화 오브젝트와 달리, **Resource는 스프링에서 빈이 아니라 값으로 취급**
- -> 리소스는 OXM이나 트랜잭션처럼 서비스를 제공해주는 것이 아니라, 단순한 정보를 가진 값으로 지정됨.
- --> 빈으로 등록한다면 리소스 타입에 따라 각기 다른 Resource 인터페이스 구현 했겠지만,
- --> 빈으로 등록하지 않으니 기껏해서 property의 value 애트리뷰트에 넣는 방법 밖에 없음 -> **단순 문자열**만 넣을 수 있음..

#### 리소스 로더
- 스프링에는 URL 클래스와 유사하게 **접두어를 이용해 Resource 오브젝트를 선언하는 방법**이 있음
- -> 문자열 안에 리소스의 종류와 리소스의 위치를 함께 표현하게 해주는 것.
- -> 이렇게 **문자열로 정의된 리소스를 실제 Resource 타입오브젝트로 변환해주는 ResourceLoader를 제공**함. 
- -> ResourceLoader도 구현 다양할 수 있으므로 아래와 같은 인터페이스를 스프링이 정의해둠
```java
package org.springframework.core.io;

public interface ResourceLoader {
    Resource getResource(String location); // location에 담긴 스트링 정보를 바탕으로 그에 적절한 Resource로 변환해줌.
    //..
}
```
##### 문자열 예시
- 아래는 ResourceLoader가 인식하는 접두어와 이를 이용해 리소스를 표현한 예시
- 접두어가 없는 경우에는 리소스 로더의 구현 방식에 따라 리소스를 가져오는 방식이 달라짐. 
- 접두어를 붙여주면 리소스 로더의 종류와 상관없이 **접두어가 의미하는 위치와 방법을 이용해 리소스 읽어옴**
- ex)
- file: -> file:/C:/temp/file.txt 
- classpath: -> classpath:file.txt 
- 없음 -> WEB-INF/test.dat -> 접두어가 없는 경우 ResourceLoader 구현에 따라 리소스 위치가 결정됨. ->ServletResouceLoader라면 서블릿컨텍스트의 루트를 기준으로 해석함
- http: -> http://www.myserver.com/test.dat -> http 프로토콜을 사용해 접근할 수 있는 웹 상의 리로스 지정. ftp도 사용 가능

##### 예시 - 스프링 애플리케이션 컨텍스트
- ResourceLoader의 대표적인 예는 스프링 애플리케이션 컨텍스트
- **애플리케이션 컨텍스트가 구현해야하는 인터페이스인 ApplicationContext는 ResourceLoader 인터페이스를 상속함**
- -> 따라서 모든 애플리케이션 컨텍스트는 리소스 로더이기도 함!
- -> 스프링 컨테이너는 리소스 로더를 다양한 목적으로 사용하고 있기 때문 
- -> 예를들어, 애플리케이션 컨텍스트가 사용할 스프링 설정정보가 담긴 XML 파일도 리소스 로더를 이용해 Resource 형태로 읽어옴!
- -> 그 밖에도 애플리케이션 컨텍스트가 외부에서 읽어오는 모든 정보는 리소스 로더를 사용하게 되어있음. 
- -> 또한 빈의 프로퍼티 값을 변환할때도 리소스 로더가 자주 사용됨. 
- --> 스프링이 제공하는 빈으로 등록 가능한 클래스에 파일을 지정해주는 프로퍼티가 존재한다면 거의 모두 Resource 탕비
- --> Resource 타입은 빈으로 등록하지 않고 property 태그의 value를 사용해 문자열로 값을 넣는데,
- --> 이 과정에서 문자열로 된 리소스 정보를 Resource 오브젝트로 변환해서 프로퍼티에 주입할 때도 애플리케이션 컨텍스트 자신이 리소스 로더로서 변환과 로딩 기능을 담당함.
- ex) myFile이라는 이름의 프로퍼티가 Resource 타입이라면 아래와 같이 다양하게 지정 가능
```
<property name="myFile" value="classpath:com/epril/myproject/myfile.txt" />
<property name="myFile" value="file:/data/myfile.txt" />
<property name="myFile" value="http://www.myserver.com/test.dat" />
```
- -> myFile 프로퍼티 입장에서는 추상화된 Resource 타입의 오브젝트로 전달 받기 때문에 리소스가 실제 어디로 존재하는지, 어떤 종류인지 상관없이 동일 방법으로 리소스 내용 읽어옴

#### Resource 이용해 XML 파일 가져오기. 
- OxmSqlService에 Resource를 적용해서 SQL매핑정보가 담긴 파일을 다양한 위치에서 가져올 수 있게 만들기
- 일단 스트링으로 되어 있떤 sqlmapFile 프로퍼티를 모두 Resource 타입으로 바꾸기 
- -> 이름도 sqlmap으로 변경. 꼭 파일에서 읽어오는 것은 아닐 수 있기 때문
- Resource 타입은 실제 소스가 어떤 것이든 상관없이 getInputStream()을 이용해 스트링으로 가져올 수 있음. 
- -> 이를 StreamSource 클래스를 이용해서 OXM 언마살러가 필요하는 Source타입으로 만들어주면 됨. 