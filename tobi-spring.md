# 오브젝트와 의존관계
## 자바빈
- 디폴트 생성자 : 자바빈은 파라미터가 없는 디폴트 생성자 갖고 있어야함. 툴이나 프레임워크에서 리플렉션 이용하여 오브젝트를 생성
- 프로퍼티 : 자바빈이 노출하는 이름을 가진 속성. 프로퍼티는 세터게터 가짐

### main()을 이용한 DAO 테스트 코드
- 모든 클래스에는 자신을 엔트리 포인트로 설정해 직접 실행이 가능하게 해주는 스태틱 메소드 main()이 있음.

## DAO의 분리 (p.61)
### 관심사의 분리
### 커넥션 만들기의 추출
```java
public class UserDao {
    public void add(User user) throws ClassNotFoundException, SQLException{
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) value(?,?,?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();;
    }

    public User get(String id) throws ClassNotFoundException, SQLException{
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "select * from users where id = ?");
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();
        rs.next();
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));

        rs.close();
        ps.close();
        c.close();

        return user;
    }

    public abstract Connection getConnection() throws ClassNotFoundException, SQLException;
}
```
#### UserDao의 관심사항
- 1. DB 연결, 2.SQL문 만들고 실행, 3.리소스close
#### 중복 코드의 메소드 추출
#### 변경사항에 대한 검증 : 리팩토링과 테스트

### DB 커넥션 만들기의 독립
- 컴파일된 소스코드만으로도 DB 커넥션 생성 방식을 변경하면서 사용하게 만들기

#### 상속을 통한 확장
- getConnection()을 추상메소드로 만들어, 서브클래스를 만든 뒤, getConnection()을 확장

### 템플릿 메소드 패턴 
- -> 이렇게 슈퍼클래스에 기본적인 로직의 흐름을 만들고 기능의 일부를
- 추상메소드나 오버라이딩이 가능한 protected메소드 등으로 만든 뒤
- **서브클래스에서 필요에 맞게 구현해서 사용하도록 하는 방법이 템플릿 메소드 패턴**
- 서브클래스에서 선택적으로 오버라이드할 수 있도록 만들어둔 메소드를 **훅(hook)메소드**라고 함.

### 팩토리 메소드 패턴
- **슈퍼클래스 코드**에서 서브클래스에서 구현할 메소드를 호출해서 필요한 타입의 오브젝트를 **가져와 사용**.
- 주로 인터페이스 타입으로 오브젝트를 리턴하므로 서브클래스에서 어떻게 오브젝트를 만들어 리턴할지, 슈퍼클래스는 관심이 없음.
- 자바에서 종종 오브젝트를 생성하는 메소드를 일반적으로 팩토리메소드라고 하는데, 팩토리 메소드 패턴의 팩토리 메소드는 의미가 다름.
- 서브클래스의 getConnection() 메소드는 어떤 Connnection 클래스의 오브젝트를 어떻게 생성할 것인지 결정하는 방법
- -> 이렇게 **서브클래스에서 구제척인 오브젝트 생성 방법을 결정하게 하는 것을 팩토리 메소드 패턴**.
![](/img/img.png)

### 상속을 사용의 문제점
- 다중상속이 불가하므로 후에 다른 목적으로 상속을 적용하기 힘듬
- 상속을 통한 상하위 클래스 관계는 생각보다 밀접하여 -> 다른 관심사임에도 긴밀한 결합을 허용하게 됨.
- -> 서브클래스는 슈퍼클래스의 기능을 직접 사용할 수 있으므로 슈퍼클래스 변경이 있을 때 서브클래스 수정이 필요할 수도 있음.
- DB커넥션 생성 코드를 다른 DAO클래스에서 적용할 수 없음 -> getConnection() 코드가 DAO클래스마다 중복될 것


## DAO의 확장 (p.71)
### 클래스의 분리
- 이번에는 상속관계도 아닌 완전히 독립적인 클래스를 만들어보기
- DB 커넥션과 관련된 부분을 서브클래스가 아니라, 별도의 클래스에 담기. 
- 이렇게 만든 클래스를 UserDao가 이용하게 하기.
![](/img/img_1.png)
- 두개의 독립된 클래스로 분리한 결과
- UserDao는 SimpleConnectionMaker클래스의 오브젝트를 만들어두고 각 메소드에서 사용
```java
public class SimpleConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException{
        Class.forName("com.mysql.jdbc.Driver");
        Connection c = DriverManager.getConnection(
                "jdbc:mysql:localhost:3306/tobi-spring");
        return c;
    }
}
```
### 문제점
- 상속을 통해 DB 커넥션 기능을 확장해서 사용하는 게 불가능해짐
- UserDao 코드가 SimpleConnectionMaker라는 특정 클래스에 종속되어 있기 때문
- simpleConnectionMaker = new SimpleConnectionMaker();
- -> 위처럼 클래스 분리 경우에도 상속처럼 자유로운 확장이 가능하게 하려면

### 인터페이스의 도입
- 두 개의 클래스가 긴밀하게 연결되어 있지 않도록 중간에 추상적인 느슨한 연결고리 만들어 주는 것.
- "추상화"란 어떤것들의 공통적인 성격을 뽑아내어 이를 따로 분리해내는 작업. 
- 자바가 추상화를 위해 제공하는 가장 유용한 도구는 바로 "인터페이스"
- 인터페이스는 자신을 구현한 클래스에 대한 구체적 정보는 감춰버림.
- -> 인터페이스로 추상화해놓은 최소한의 통로를 통해 접근하는 쪽에서 오브젝트를 만들 떄 클래스가 무엇인지 몰라도 됨

```java
import java.sql.SQLException;

public interface ConnectionMaker {
    public Connection makeConnection() throws ClassNotFoundException, SQLException;
}
```

```java
public class UserDao {
    private ConnectionMaker connectionMaker;

    public UserDao() {
        connectionMaker = new DConnectionMaker(); // 클래스 이름이 나오는 중..
    }
    //...
}
```
- 위의 코드에서는 DConnection이라는 구현 클래스이름이 보임
- 인터페이스를 사용하더라도 DConnection 이라는 클래스 생성 로직이 남아 있
- -> 초기에 한 번 어떤 클래스를 사용할지 결정하는 코드가 남아있음


### 관계설정 책임의 분리
- new DConnectionMaker() 라는 코드는 그 자체로 독립적인 관심사를 담고 있음
- -> UserDao가 어떤 ConnectionMaker 구현 클래스의 오브젝트를 이용하게 할지를 결정하는 것.
- -> UserDao와 UserDao가 사용할 ConnectionMaker의 특정 구현 클래스의 관계를 설정해주는 관심
- **UserDao를 사용하는 클라이언트 오브젝트에서** UserDao와 ConnectionMaker 관계를 결정해주는 로직을 두기

#### 오브젝트와 오브젝트의 관계 설정
- 오브젝트 사이의 관계는 **런타임 시** 한쪽이 다른 오브젝트 레퍼런스를 갖고 있는 방식으로 만들어짐.
- ex) DConnectionMaker의 오브젝트의 레퍼런스를 UserDao의 connectionMaker 변수에 넣어서 사용하게 함으로써
- -> 두 개의 오브젝트가 '사용'이라는 관계를 맺게 해줌.
- connetionMaker = new DConnectionMaker();
- -> 오브젝트 사이의 관계가 만들어지려면 일단 만들어진 오브젝트가 있어야함
- -> 위처럼 생성자 직접 호출 방법도 있지만, 외부에서 만들어주는 것을 가져오는 방법도 있음.
- -> 외부에서 만든 오브젝트를 전달받으려면 메소드 파라미터나 생성자 파라미터를 이용하면 됨.

#### 파라미터의 타입을 전달받을 오브젝트의 인터페이스로 선언했을 경우
- -> 파라미터로 전달되는 오브젝트의 클래스는 해당 인터페이스를 구현만했다면 어떤 것인지 상관 없음.

#### 의존관계
- UserDao 오브젝트가 DConnectionManager 오브젝트를 사용하게 하려면 
- -> 두 클래스의 오브젝트 사이에 런타임 사용관계 또는, 링크 또는 의존관계라고 불리는 관계를 맺어주면 됨

#### 클라이언트 오브젝트
- UserDao도 아니고, ConnectionMaker 구현 클래스도 아닌 
- 제 3의 오브젝트인 UserDao의 클라이언트 오브젝트는 무슨 역할?
- -> 클래스-인터페이스-클래스 구조를 이용해 런타임오브젝트 관계를 갖게 하는 구조로 만들어주는게 클라이언트의 책임
```java
public class UserDaoTest { // 클라이언트 오브젝트
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao dao = new UserDao(connectionMaker); //의존관계 설정 책임
        //...
    }
}
```

### 원칙과 패턴
- 위의 개선과정에서 적용된 원칙과 패턴
#### 개방 폐쇄 원칙(OCP)
- 클래스나 모듈은 확장에는 열려있고 변경에는 닫혀있어야 한다.
- -> UserDao는 DB연결 방법이라는 기능확장에는 열려있으나, 핵심 구현 코드는 영향 받지 않음.
#### 높은 응집도와 낮은 결합도
