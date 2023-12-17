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