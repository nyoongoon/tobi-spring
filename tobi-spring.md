# 1장 오브젝트와 의존관계

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
    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) value(?,?,?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();
        ;
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
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

-
    1. DB 연결, 2.SQL문 만들고 실행, 3.리소스close

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
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
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

- "응집도"가 높다는 건 모듈, 클래스가 하나의 책임 또는 관심사에만 집중되어 있다는 뜻.
- "결합도"는 하나의 오브젝트가 변경이 일어날 때에 관계를 맺고 있는 다른 오브젝트에게 변화를 요구하는 정도

#### 전략 패턴 (-> 대체 가능한 전략)

- 개선한 UserDaoTest-UserDao-ConnectionMaker 구조를 디자인 패턴으로 보면
- -> 전략 패턴에 해당함. - 개방 폐쇄 원칙 실현에 가장 잘 들어맞음
- **자신의 기능맥락(context) <- 에서 필요에 따라 변경가능한 알고리즘(기능)을 외부로 분리**
- "전략 패턴"은 자신의 기능맥락(context)에서, 필요에 따라 변경이 필요한 알고리즘을
- -> 인터페이스를 통해 통째로 외부로 분리시키고
- -> 이를 구현한 알고리즘 클래스를 필요에 따라 바꿔서 사용

##### 전략패턴 예시

- 위에서 구현한 UserDao는 전략패턴의 **컨텍스트**
- 컨텍스트는 자신의 기능을 수행하는데 필요한 기능 중에서
- -> "변경 가능"한 DB 연결방식이라는 알고리즘을 ConnectionMaker라는 인터페이스로 정의,
- -> 이를 구현한 클래스, 즉 전략을 바꿔가면서 사용할 수 있게 분리함.
- -> 전략패턴에서 "클라이언트"는 컨텍스트가 사용할 전략을 컨텍스트의 생성자를 통해 제공해주는 게 일반적

## 제어의 역전(IoC) - p.89

- 위의 UserDaoTest의 기능을 분리하기
- -> UserDao와 ConnectionMaker 구현 클래스의 오브젝트를 만드는 것과,
- -> 그렇게 만들어진 두 개의 오브젝트가 연결돼서 사용될 수 있게 관계 맺어주는 것.

### 팩토리

- "팩토리" 오브젝트로 분리
- -> 이 클래스의 역할은 객체의 생성 방법을 결절하고 그렇게 만들어진 오브젝트를 돌려주는 것.
- cf) 추상팩토리 패턴, 팩토리 메소드 패턴과는 다름

```java
public class DaoFactory {
    public UserDao userDaoJdbc() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao userDaoJdbc = new UserDao(connectionMaker);
        return userDaoJdbc;
    }
}
```

```java
public class UserDaoTest {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        UserDao dao = new DaoFactory().userDaoJdbc();
        //...
    }
}
```

#### 설계도로서의 팩토리

- 팩토리 클래스는 로직 오브젝트를 구성하고, 관계를 정의하는 책임
- -> 애플리케이션을 구성하는 컴포넌트의 구조와 관계를 정의한 설계도 같은 역할
- -> 어떤 오브젝트가 어떤 오브젝트를 사용하는지 정의해놓은 코드
- -> 팩토리 클래스를 분리하여, 컴포넌트역할을 하는 오브젝트(로직)와, 구조를 결정하는 오브젝트(팩토리)를 분리해냄

#### 오브젝트 팩토리의 활용

- ConnectionMaker의 구현 클래스를 결정하고, 오브젝트를 만드는 코드를 별도로 분리

```java
public class DaoFactory {
    public UserDao userDaoJdbc() {
        return new UserDao(connectionMaker());
    }

    public AccountDao accountDao() {
        return new AccountDao(connectionMaker());
    }

    public MessageDao messageDao() {
        return new AccountDao(connectionMaker());
    }

    public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
    }
}
```

### 제어권의 이전을 통한 제어관계 역전

- 제어의 역전에서는 오브젝트가 자신이 사용할 오브젝트를 스스로 선택하거나 생성하지 않음.
- 프로그램의 시작을 담당하는 main()과 같은 엔트리 포인트를 제외하면
- -> 모든 오브젝트는 이렇게 위임받은 제어 권한을 갖는 특별한 오브젝트에 의해 결정됨
- ex) 템플릿 메소드는 제어의 역전이라는 개념을 활용한 것.

#### 라이브러리 vs 프레임워크

- 코드가 라이브러리를 사용. 애플리케이션 흐름 직접 제어
- 코드가 프레임워크에 의해 사용됨. 프레임워크가 흐름 제어.

#### 제어의 역전에서 필요 요소

- 프레임워크 또는 컨테이너와 같이 애플리케이션 컴포넌트의 생성과 관계 설정, 사용, 생명주기 관리 등을 관장하는 존재가 필요.

## 스프링의 IoC

- 스프링의 핵심은 "빈 팩토리" 또는 "어플리케이션 컨텍스트"라고 불리는 것.

### 오브젝트 팩토리를 이용한 스프링 IoC

- DaoFactory를 스프링에서 사용이 가능하도록 변신시키기..
- **빈**이란 스프링이 제어권을 가지고, 직접 만들고, 관계를 부여하는 오브젝트
- 스프링 빈은 스프링 컨테이너가 생성과 관계설정, 사용 등을 제어해주는 제어의 역전이 적용된 오브젝트.
- **빈 팩토리**란 스프링에서는 빈의 생성과 관계 설정같은 제어를 담당하는 IoC 오브젝트
- -> 보통 빈팩토리보다 이를 더 확장한 **애플리케이션 컨텍스트**를 주로 사용
- -> 애플리케이션 컨텍스트는 IoC방식을 따라 만들어진 일종의 빈 팩토리.

#### 애플리케이션 컨텍스트

- 애플리케이션 컨텍스트는 별도의 정보를 참고해서 빈(오브젝트)의 생성, 관계설정 등의 제어 작업을 총괄.
- **별도의 설정정보**를 담고 있는 무엇인가를 가져와 이를 활용하는 범용적인 IoC 엔진
- -> 그 자체로는 애플리케이션의 로직을 담당하지 않지만
- -> IoC를 이용해 애플리케이션 컴포넌트를 생성하고, 사용할 관계를 맺어주는 책임

#### DaoFactory를 사용하는 애플리케이션 컨텍스트

- DaoFactory를 스프링의 빈 팩토리가 사용할 수 있는 본격적인 설정정보로 만들어보기
- -> 먼저 스프링이 빈 팩토리를 위한 오브젝트 설정을 담당하는 클래스라고 인식할 수 있도록 @Configuration 추가
- -> 그리고 오브젝트를 만들어주는 메소드에 @Bean이라는 애노테이션을 붙여줌.

```java

@Configuration
public class DaoFactory {
    @Bean
    public UserDao userDaoJdbc() {
        return new UserDao(connectionMaker());
    }

    @Bean
    public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
    }
}
```

- 이제 DaoFactory를 설정정보로 사용하는 애플리케이션 컨텍스트를 만들기.
- **애플리케이션 컨텍스트는** ApplicationContext 타입의 오브젝트.
- ApplicationContext를 구현한 클래스는 여러가지가 있는데,
- DaoFactory처럼 @Configuration이 붙은 자바 코드를 설정정보로 사용하려면
- AnnotaionCaonfigApplicationContext를 이용하면 됨.
- 애플리케이션 컨텍스트를 만들 때 생성자 파라미터로 DaoFactory 클래스를 넣어줌.
- -> 이렇게 준비된 ApplicationContext의 getBean()이라는 메소드를 이용해 UserDao의 오브젝트를 가져올 수 있음.

```java
public class UserDaoTest {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        ApplicationContext context =
                new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao dao = context.getBean("userDaoJdbc", UserDao.class);
        //...
    }
}
```

- -> getBean()의 파라미터인 "userDaoJdbc"는 ApplicationContext에 등록된 빈의 이름
- @Bean이라는 애노테이션을 userDao라는 이름의 메소드에 붙였는데, 메소드 이름이 바로 빈의 이름이 됨

##### 메소드 이름을 빈의 이름으로 사용하는 이유

- 그런데 UserDao를 가져오는 메소드는 하나뿐인데 이름을 사용하는 이유?
- -> 오브젝트 생성하는 방식이나 구성을 다르게 가져가는 메소드를 추가할 수 있음.

### 애플리케이션 컨텍스트의 동작방식

- 기존 오브젝트 팩토리에 대응되는 것이 스프링 애플리케이션 컨텍스트
- == IoC 컨테이너 == 스프링 컨테이너 == 빈팩토리
- ApplicaionContext는 빈팩토리가 구현하는 BeanFactory 인터페이스를 상속했으므로
- -> 애플리케이션 컨텍스트는 일종의 빈팩토리.

#### 생성정보와 연관관계 정보 (설정정보)

- ApplicationContext에는 생성정보와 연관관계 정보를 별도의 설정정보를 통해 얻음
- 떄로 외부의 오브젝트 팩토리에 그 작업을 위임하고 결과를 가져다 사용하기도 함.

##### ㄴ> @Configuration

- @Configuration이 붙은 클래스(여기서 DaoFactory)는 애플리케이션 컨텍스트가 활용하는
- -> **IoC 설정정보**
- -> 내부적으로 애플리케이션 컨텍스트가 빈생성메소드(@Bean)을 호출해서 가져온 것을
- -> 클라이언트가 getBean()으로 요청할 때 전달해준다.

#### 애플리케이션 컨텍스트 사용 장점

- 클라이언트는 구체적인 팩토리 클래스를 알 필요가 없다.
- 종합 IoC 서비스를 제공해줌 -> 다양한 추가 기능 제공
- 빈을 검색하는 다양한 방법을 제공.

### 스프링 IoC 용어 정리

#### 빈

- 스프링이 IoC 방식으로 생성과 제어를 담당하는 오브젝트

#### 빈팩토리

- 스프링의 IoC를 담당하는 핵심 컨테이너
- 빈 등록, 생성, 조회, 전달, 관리 등
- 보통은 빈팩토리를 바로 사용하지 않고 -> 이를 확장한 애플리케이션 컨텍스트를 이용.
- BeanFactory라고 붙여쓰면 빈팩토리가 구현하고 있는 가장 기본적인 인터페이스 - getBean() 등이 정의됨.

#### 애플리케이션 컨텍스트

- 빈팩토리를 확장한 IoC 컨테이너
- 주로 빈팩토리라고 부를때는 주로 빈의 생성과 제어의 관점에서 이야기 하는것.
- 애플리케이션 컨텍스트는 스프링의 모든 기능을 포함하여 이야기 것
- ApplicationContext는 애플리케이션 컨텍스트가 구현해야하는 기본 인터페이스
- ApplicationContext는 BeanFactory를 상속함.

#### 설정정보/설정메타정보

- 스프링 설정정보란 애플리케이션 컨텍스트 또는 빈팩토리가 IoC를 적용하기 위해 사용하는 메타 정보를 말함
- 컨테이너의 어떤 기능을 세팅하거나 조정하는 경우도 사용하지만
- -> 주로 IoC 컨테이너에 의해 관리되는 애플리케이션 오브젝트를 생성하고 구성할때 사용함.

#### 컨테이너 또는 IoC 컨테이너

- 컨테이너라는 말 차제가 IoC의 개념을 갖고 있음
- 컨테이너 애플리케이션 컨텍스트보다 추상적인 표함
- 애플리케이션 컨텍스트는 ApplicationContext 인터페이스를 구현한 오브젝트를 가리키기도 함.
- 애플리케이션 컨텍스트 오브젝트는 보통 여러개가 만들어져 하나의 애플리케이션에서 사용됨. -> 통틀어서 스프링 컨테이너

## 싱글톤 레지스트리와 오브젝트 스코드

- DacFactory를 사용하는 것과 달리
- 애플리케이션 컨텍스트에서 getBean() 메소드를 이용해 오브젝트를 가져오는 것은
- 매번 동일한 오브젝트를 돌려줌.

### 싱글톤 레지스트리로서의 애플리케이션 컨텍스트

- 애플리케이션 컨텍스트는 IoC 임과 동시에
- -> 싱글톤을 저장하고 관리하는 싱글톤 레지스트리
- cf) 디자인 패턴의 싱글톤 패턴과 비슷하지만 다름.

#### 서버 애플리케이션과 싱글톤

- 스프링은 엔터프라이즈 서버 환경으로 고안된 기술
- -> 요청이 올떄마다 오브젝트를 새로 만들어진다고 하면 많은 부하가 생김.

##### 서비스 오브젝트

- 엔터프라이즈 분야에서는 **서비스 오브젝트** 개념이 일찍부터 있었음
- -> **서블릿**은 자바 엔터프라이즈 기술의 가장 기본이 되는 서비스 오브젝트
- 강제하진 않지만 서블릿은 대부분 멀티스레드 환경에서 싱글톤으로 동작함.

#### 싱글톤 패턴

- 서비환경에서 서비스 싱글톤의 사용이 권장됨
- 하지만 디자인 패턴에 소개된 싱글톤은 문제가 있음 -> 비판이 많음
- 하나만 만들어지는 클래스의 오브젝트는 애플리케이션내에서 전역적으로 접근 가능.

##### 싱글톤 패턴 문제점

- ex) UserDao를 싱글톤으로 만들어보기

```java
import org.user.dao.ConnectionMaker;

public class UserDao {
    private static UserDao INSTANCE;

    private UserDao(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
    }

    public static synchronized UserDao getInstance() {
        if (INSTANCE == null) INSTANCE = new UserDao( ???);
        return INSTANCE;
    }
}
```

- 상속 불가 : <- private 생성자
- 객체지향 특징 적용 어려움 : 스태틱 필드와 메소드
- 테스트 어려움 : 만들어지는 방식이 제한적임, 사용할 오브젝트 주입 어려움
- 서버환경에서는 싱글톤이 하나만 만들어지는 것을 보장 못함
- : -> 서버에서 클래스 로더를 어떻게 구성하고 있느냐 따라 하나 이상 오브젝트가 만들어질수 있음.
- 싱글톤의 사용은 전역 상태를 만들 수 있기 때문에 바람직하지 못함.
- : -> 싱글톤은 사용하는 클라이언트가 정해져있지 않음. 스태틱 메소드를 이용해서 어디서든 쉽게 접근.
- -> 전역상태로 사용되기 쉬움 -> 객체지향에서 권장X

#### 싱글톤 레지스트리

- 스프링은 서버환경에서 싱글톤이 만들어져서 서비스 오브젝트 방식으로 사용되는 것은 지지
- -> 자바 기본적인 싱글톤은 단점이 있기 때문에 -> 직접 싱글톤 형태의 오브젝트를 만들고 관리하는 기능 제공
- -> **싱글톤 레지스트리**

##### 스프링 싱글톤 레지스트리의 장점

- -> 스태틱 메소드와 private 생성자를 사용해야하는 비정상 클래스가 아니라
- -> **평범한 자바 클래스를 싱글톤으로 활용**하게 해준다는 점.
- -> 평범한 자바 클래스라도 IoC 방식의 컨테이너를 사용해서 생성과 관계설정, 사용등에 대한 제어권을 컨테이너에게 넘기면
- -> 손쉽게 싱글톤 방식으로 만들어져 관리되게 할 수 있음.
- -> 오브젝트 생성에 관한 모든 권한은 IoC 기능을 제공하는 애플리케이션 컨텍스트에게 있기 때문.

##### 스프링의 싱글톤 레지스트리

- **스프링의 싱글톤 레지스트리** 덕분에 싱글톤 방식으로 사용될 애플리케이션 클래스라도 **pulbic 생성자를 가질 수 있음**.
- -> 테스트 환경에서 **자유롭게 오브젝트를 만들 수도 있고**, 테스트를 위한 목오브젝트로 대체하느 ㄴ것도 간단함.
- -> **생성자 파라미터를 이용해서 사용할 오브젝트를 넣어주게** 할 수도 있음
- 스프링은 IoC 컨테이너 뿐만 아니라, 싱글톤 레지스트리임을 기억하기.

#### 싱글톤과 오브젝트의 상태

- 멀티스레드 환경에서 상태관리에 주의해야함.
- -> 싱글톤이 멀티스레드 환경에서 서비스 형태의 오브젝트로 사용되는 경우
- -> 상태정보를 내부에 갖고 있지 않은 **무상태방식**으로 만들어져야함.
- -> 싱글토는 기본적으로 인스턴스 필드의 값을 변경하고 유지하는 상태유지 방식으로 만들지 않음.

##### 상태가 없는 방식으로 정보 다루기

- 상태가 없는 방식으로 클래스 만드는 경우, 요청 정보나, 리소스로부터 생성한 정보 어떻게 다뤄야할까?
- -> 파라미터와 로컬변수, 리턴값 등을 이용.
- -> 메소드 파라미터나, 메소드안에서 생성되는 로컬변수는 매변 새로운 값을 저장할 독립적인 공간이 만들어지기 때문 덮어씌워질 가능성 없음
- 문제 경우 예시 코드)

```java
public class UserDao {
    private ConnectionMaker connectionMaker; //초기 설정하면 바뀌지 않는 읽기전용 변수(Bean)는 괜찮
    private Connection c; //싱글톤에서 인스턴스 필드로 선언하면 문제 발생
    private User user;
}
```

- -> 기본 UserDao처럼 개별적으로 바뀌는 정보는 로컬 변수로 정의하거나, 파라미터로 주고받으면서 사용하게 해야함.
- connectionMaker 처럼 다른 싱글톤 빈을 저장하려는 용도라면 가능함.
- -> **빈 객체의 인스턴스 필드에는 또다른 빈 객체가 선언되거나 읽기전용 변수 사용 가능 !!!**
- -> 단순한 읽기 전용이라면 static final이나 final로 선언하는 편이 나음

### 스프링 빈의 스코프

- 빈의 스코프 : 빈이 생성되고, 존재하고, 적용되는 범위
- 스프링 빈의 기본 스코프는 싱글톤.
- **싱글콘 스코프**는 컨테이너 내에 한 개의 오브젝트만 만들어져서, 강제로 제거하지 않는 한 스프링 컨테이너가 존재하는 동안 계쏙 유지
- 싱글톤외의 스코프를 가질 수 있음
- -> 프로토타입 스코프. -> 컨테이너에 빈을 요청할떄마다 매번 새로운 오브젝트를 만들어줌.
- -> 요청 스코프 -> 웹을 통해 새로운 HTTP 요청이 생길때마다 생성됨
- -> 세션 스코프 -> 웹의 세션과 스코프가 유사.

## 의존관계 주입(DI) == 의존관계 설정

### 제어의 역전과 의존관계 주입

- IoC라는 용어는 폭넓게 사용됨
- **스프링의 IoC 방식의 핵심을 짚어주는 의존 관계 주입이**라는 명확한 명칭
- 스프링 IoC 기능의 대표적인 동작원리는 주로 의존관계 주입이라고 불림
- -> DI 컨테이너라고 더 많이 불림
- DI는 **오브젝트 레퍼런스를 외부로부터 제공(주입)받고** 이를 통해 다른 오브젝트와 다이나믹한 의존관계가 만들어지는 것이 핵심
- -> 엄밀히 오브젝트는 다른 오브젝트에 주입할 수 있는 것이 아니라, **오브젝트의 레퍼런스가 전달될 뿐**.

### 런타임 의존관계 설정

#### 의존관계

- A가 B에 의존하고 있다고 할 때
- 의존한다는 건 **의존 대상인 B가 변하면 그에 의존하는 A에 영향을 미친다**는 것.
- -> 의존 관계는 방향성이 있음. -> A가 변해도 B는 영향 받지 않음.

#### UserDao의 의존관계

- 런타임 의존관계 또는 오브젝트 의존 관계는 설계시점의 의존관계가 실체화 된 것.
- 런타임 의존관계는 모델링 시점의 의존관계와 성격이 다름.
- -> 인터페이스를 통해 설계시점에 느슨한 의존관계를 갖는 경우에 UserDao의 오브젝트가
- -> 런타임시에 사용할 오브젝트가 어떤 클래스로 만든 것인지 미리 알 수가 없음
- -> 이렇게 프로그램이 시작되고 UserDao 오브젝트가 만들어지고나서 런타임 시에 의존관계를 맺는 대상,
- -> 즉, 실제 사용 대상인 오브젝트를 **의존 오브젝트**라고 말함.
- -> **의존관계주입**은 이렇게 구체적인 의존 오브젝트와
- -> 그것을 사용할 주체, 보통 클라이언트라고 부르는 오브젝트를 **런타임시에 연결해주는 작업**을 말함.

##### 의존관계 주입 3가지 조건

- 클래스 모델이나 코드에는 런타임 시점의 의존관계가 드러나지 않는다. 그러기 위해서는 **인터페이스에만 의존**하고 있어야한다.
- **런타임 시점의 의존관계**는 **컨테이너나 팩토리 같은 제 3의 존재가 결정**한다.
- **의존관계**는 사용할 오브젝트에 대한 **레퍼런스(DaoFactory)를 외부에서 제공(주입)**해줌으로써 만들어진다.
- -> 의존관계 주입의 핵심은 설계 시점에는 알지 못했던 두 오브젝트의 관계를 맺도록 도와주는 제 3의 존재가 있다는 것.

#### UserDao의 의존관계 주입

- ex)관계설정 책임 분리 전의 생성자

```
public UserDao(){
    connectionMake = new DConnectionMaker();
}
```

- IoC 방식을 써서 UserDao로부터 런타임 의존관계를 드러내는 코드를 제거하고,
- -> 제3의존재(DaoFactory)에 런타임 의존관계 결정 권한을 위임함.
- ApplicationContext는 의존관계 레퍼런스인 DaoFactory를 참고하여, 런타임 시점에
- -> UserDao가 사용할 ConnectionMaker 타입의 오브젝트를 결정하고, 이를 결정한 후에,
- -> UserDao 생성자 파라미터에 주입해서 UserDao가 DConnectionMaker의 오브젝트와 런타임 의존관계를 맺게 해줌.

#### DI 컨테이너 의존관계 주입 예시코드

- DI컨테이너는 자신이 결정한 의존관계를 맺어줄 클래스의 오브젝트를 만들고 이 생성자의 파라미터로 오브젝트의 레퍼런스를 전달해줌.
- -> 이렇게 생성자 파라미터를 통해 전달받은 런타임 의존관계를 갖는 오브젝트는 인스턴스 변수에 저장해둠.

```java
public class UserDao {
    private ConnectionMaker connectionMaker;

    public UserDao(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
    }
}
```

### 의존관계 검색과 주입

- 런타임시에 의존관계를 결정하는 점에서 의존관계 주입과 비슷하지만
- -> 의존관계를 맺는 방식이 외부로부터의 주입이 아니라,
- -> **스스로 검색을 이용**하기 떄문에 **의존관계 검색**이라고 불리는 것도 있다.
- 의존관계 검색은 자신이 필요로 하는 오브젝트를 능동적으로 찾음
- -> 런타임시 의존관계를 맺을 오브젝트 결정하는 것과, 오브젝트 생성 작업은 외부 컨테이너에서 IoC로 맡기지만
- -> 가져올 때는 주입 대신 스스로 컨테이너에게 요청하는 방법을 사용.

```
// 생성자
public UserDao(){
    AnnotationConfigApplicationContext context =
        new AnnotatinoConfigApplicaionContext(DaoFactory.class);
    this.connectionMaker = context.getBean("connectionMaker", ConnectionMaker.class);
}
```

- 이렇게 해도 여전히 자신이 어떤 ConnectionMaker 오브젝트를 사용할지 알지 못함.
- -> 적용 방법이 주입이 아니라 스스로 IoC컨테이너에게 검색 요청
- 의존관계 검색은 주로 테스트 코드에서 사용.

#### 의존관계 검색과 의존관계 주입 주요 차이점

- 의존관계 검색방식에서는 검색하는 오브젝트는 자신이 스프링 빈일 필요는 겂음.
- **의존관계 주입에서는** UserDao와 ConnectionMaker 사이에 DI가 적용되려면
- UserDao와 ConnectionMaker **둘다 빈 오브젝트여야함. !**
- -> 컨테이너가 ConnectionMaker 오브젝트를 주입해주려먼
- -> UserDao에 대한 생성과 초기화 권한을 갖고 있어야하고
- -> 그러려면 UserDao는 IoC 방식으로 컨테이너에서 생성되는 오브젝트여야함.
- -> ** DI를 원하는 오브젝트는 먼저 자신이 빈이 되어야함!! **

##### DI 받는다

- 외부에서 파라미터로 오브젝트를 넘겨줬다고 다 DI가 아님
- -> 주입받는 메소드 파라미터가 이미 특정 타입으로 고정되어 있다면 DI 일어날 수 없음
- -> DI에서 말하는 주입은 다이나믹하게 구현클래스를 결정해서 제공받을 수 있도록 **인터페이스 타입의 파라미터를 통해 이뤄져야함** !

### 메소드를 이용한 의존관계 주입

- 수정자 메소드(setter)를 이용한 주입 -> xml로 의존관계 정보 만들 떄 편리..?
- 일반 메소드를 이용한 주입

## XML을 이용한 설정

- 컴파일 같은 별도의 빌드 작업이 없다는 장점.

### XML 설정

- 스프링 애플리케이션 컨텍스트는 XML에 담긴 DI정보를 활용할 수 있음.
- DI정보가 담긴 XML 파일은 \<beans\>를 루트 엘리먼트로 사용
- @Configuration을 \<beans\>, @Bean을 \<bean\>에 대응

#### @Bean의 DI 정보 세가지

- 빈의 이름 : @Bean메소드 이름이 빈의 이름
- 빈의 클래스 : 빈 오브젝트를 어떤 클래스를 이용해서 만들지
- 빈의 의존 오브젝트 : 빈의 생성자나 메소드를 통해 의존 오브젝트를 넣어줌
- -> XML에서도 이 세가지 정보를 정의.
- -> 세터 주입인 경우, 세터 메소드는 프로퍼티가 됨(set제외부분)

```
userDaoJdbc.setConnectionMaker(connectionMaker());
=>
<property name="connectionMaker" ref="connectionMaker" />
```

#### XML의 의존관계 주입 정보

```xml

<beans>
    <bean id="connectionMaker" class="springbook.user.daoDConnectionMaker"/>
    <bean id="userDaoJdbc" class="springbook.user.dao.UserDao">
        <property name="connectionMaker" ref="connectionMaker"/>
    </bean>
</beans>
```

- \<property\>태그의 name과 ref 차이점 주의
- name 애트리뷰트는 DI에 사용할 수정자 메소드의 프로퍼티 이름.
- ref 애트리뷰트는 주입할 오브젝트를 정의한 빈의 ID.

### XML을 이용하는 애플리케이션 컨텍스트

- XML에서 빈의 의존관계 정보를 이용하는 작업에는
- -> GenericXmlApplicationContext를 사용.
- GenericXmlApplicationContext의 생성자 파라미터로 XML파일의 클래스패스를 지정해주면 됨.
- -> 애플리케이션 컨텍스트가 사용하는 XML 설정파일의 이름은 관례에 따라 applicationContext.xml이라고 만듬

```
ApplicationContext context = 
    new GenericXmlApplicationContext("applicatoinContext.xml");
```

- ClassPathXmlApplicationContext는 XML 파일을 클래스패스에서 가져올 때 편리한 기능 추가
- -> 특정 상황이 아니라면 GenericXmlApplicationContext이 무난

## **DataSource 인터페이스**로 변환

- 자바에서는 DB 커넥션을 가져오는 오브젝트의 기능을 추상화해서 비슷한 용도로 사용할 수 있게 만들어진
- -> DataSource라는 인터페이스가 이미 존재함.
- -> 일반적으로 DataSource 인터페이스는 많은 메소드를 갖고 있어서 직접 구현하기엔 부담스러울 수 있음
- -> 구현된 DataSource 구현 클래스가 많이 존재.
- -> 대개 DataSource 인터페이스에서 실제로 관심 가질 것은 getConnection() 하나.

```java
import java.sql.SQLException;

public interface DataSource extends CommonDataSource, Wrapper {
    Connection getConnection() throws SQLException;
    // ...
}
```

- DataSource 구현 클래스 사용하도록 UserDao 리팩토링하기
- DataSource의 getConntection()은 SqlException만 던지기 떄문에
- -> makeConnection()메소드의 throws에 선언했던 ClassNotFoundException은 제거해도 됨.

```java
import javax.sql.DataSource;
import java.sql.SQLException;

public class UserDao {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(User user) throws SQLException {
        Connection c = dataSource.getConnection();
    }
    // ...
}
```

- 단순한 구현 클래스인 SimpleConnectionMaker 사용

```java

@Configuration
public class DaoFactory {
    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

        dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
        dataSource.setUrl("jdbc:mysql://localhost/springbook");
        dataSource.setUsername("spring");
        dataSource.setPassword("book");

        return dataSource;
    }

    @Bean
    public UserDao userDaoJdbc() {
//        return new UserDao(connectionMaker());
        UserDao userDaoJdbc = new UserDao();
        userDaoJdbc.setDataSource(dataSource());
        return userDaoJdbc;
    }
}
```

### XML 설정 방식

- dataSource라는 이름의 \<bean\>을 등록하기

```xml

<bean id="dataSource"
      class="org.springframework.jdbc.datasource.SimpleDriverDataSource"/>
```

- -> 하지만 위의 설정으로 SimpleDriverDataSource의 오브젝트를 만드는 것까지는 가능하지만,
- -> setter로 넣어준 DB접속정보는 나타나있지 않음
- -> setter의 파라미터 값은 빈이 아니므로 기존처럼 \<property\> 태그로 넣어줄 수 없음.
- -> **xml는 어떻게 dataSource() 메소드처럼 DB 연결정보를 Class타입 오브젝트나 텍스트 값으로 넣어줄 수 있을까?**

### 프로퍼티 값의 주입 (세터 값 주입)

- 이렇게 다른 빈 오브젝트의 레퍼런스가 아닌 단순 정보(DB연결정보 - Class타입 오브젝트나 텍스트)도
- -> 초기화 과정에서 세터 메소드에 넣을 수 있음
- -> @Configuraion 클래스 외부에서 DB정보 같이 변경 가능한 정보를 설정할 수 있도록..
- 이렇게 텍스트나 단순 오브젝트 등을 수정자 메소드에 넣어주는 것을 스프링에서는 **값 주입**이라고 함. (일종의 DI)
- -> 빈 오브젝트의 레퍼런스가 아니라, 단순 값을 주입해주는 것이기 때문에 value 애트리뷰트 사용

```
dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
dataSource.setUrl("jdbc:mysql://localhost/springbook");
dataSource.setUsername("spring");
dataSource.setPassword("book");
```

```
<property name="driverClass" value="com.mysql.jdbc.Driver" />
<property name="url" value="jdbc:mysql://localhost/springbook" />
<property name="username" value="spring" />
<property name="password" value="book" />
```

#### value 값의 자동 변환

- driverClass는 스트링 타입이 아니라, java.lang.Class 타입. -> xml에서는 타입정보 없이 텍스트 형태로 value에 들어가 있음.
- -> 스프링이 setter 파라미터 타입을 참고해서 변환해줌.

```
//내부적으로 변환되는 코드
Class driverClass = Class.forName("com.mysql.jdbc.Driver");
dataSource.setDriverClass(driverClass);
```

- -> 스프링은 value에 지정된 텍스트 값을 적절한 자바타입으로 변환해줌.

# 2장 테스트

- 스프링에서 가장 중요한 가치는 "객체지향"과 "테스트"

## 테스트의 유용성

### 작은 단위의 테스트

- 테스트하고자 하는 대상이 명확하다면 그 대상에만 집중해서 테스트하는 것이 바람직. -> 관심사의 분리
- 작은 단위의 코드에 대해 테스트를 수행한 것을 단위테스트라고 함.
- -> 단위의 범위는 하나의 관심에 집중해서 효율적으로 테스트할만 한 범위의 단위.
- -> DB의 상태를 테스트가 관장하고 있다면 이것도 단위테스트 -> 통제할 수 없는 외부 리소스에 의존하면 테스트는 단위테스트가 아님.
- 단위 테스트는 주로 개발자가 만든 코드를 스스로 확인하기 위해 사용 -> 개발자 테스트, 프로그래머 테스트 라고도 함.

### 자동수행 테스트 코드

- 테스트는 자동으로 수행되도록 코드로 만들어지는 것이 중요함.

## UserDaoTest의 문제점

- 수동 확인 작업의 번거로움
- 실행 작업의 번거로움

## UserDaoTest 개선

### 테스트 검증의 자동화

- 테스트 결과 두가지로 구분 가능
-
    1. 테스트 진행동안 에러가 발생해서 실패 경우 -> 콘솔에 에러미시지와 호출 스택정보가 자동 출력됨
-
    2. 결과가 기대한 것과 다르게 나오는 경우 -> 별도의 확인 작업과 그 결과과 있어야함

## Junit 테스트로 전환

```
assertThat(user2.getName(), is(user.getName()));
```

- assertThat()는 첫 번째 파라미터의 값을 뒤에 나오는 매처(matcher)라고 불리는 조건으로 비교해서
- -> 일치하면 다음으로 넘어가고, 아니면 실패하도록 만들어줌
- -> is()는 매처의 일종으로 equals()로 비교하는 기능

## 테스트 결과의 일관성

- DB를 일일이 삭제하고 있어서,
- -> 테스트가 외부 상태에 따라 성공하기도 하고, 실패하기도 한다는 점.
- -> 테스트를 마치고 나면 테스트가 등록한 사용자 정보를 삭제해서, 테스트를 수행하기 이전 상태로 만들어 주기.
- -> 코드에 변경사항이 없다면 테스트는 항상 동일한 결과를 내야함.
- -> 외부 환경에 영향을 받지 말아야하고, 실행 순서를 바꿔도 동일한 결과가 보장되어야함.

## 포괄적인 테스트

### get() 예외조건에 대한 테스트

- get 결과과 없는 경우 예외 던지기 -> 예외 클래스 작성.(스프링 미리 정의된 예외 클래스 사용 가능)
- -> 예외 테스트 기능 사용

### 테스트 작성 시 실수

- 성공하는 테스트만 골라서 만드는 것. -> 실패하는 테스트 먼저 만들기

## 테스트가 이끄는 개발

### 기능설계를 위한 테스트

- 테스트할 코드도 없는데 어떻게 테스트를 만들 수 있을까?
- -> 추가하고 싶은 기능을 코드로 표현 시도...
- -> getUserFailure() 테스트에는 만들고 싶은 기능에 대한 조건과 행위, 결과에 대한 내용이 잘 표현되어 있음.

### 테스트 주도 개발

- "실패한 테스트를 성공시키기 위한 목적이 아닌 코드를 만들지 않는다."
- TDD에서는 테스트를 작성하고 이를 성공시키는 코드를 만드는 작업의 주기를 가능한 짧게 가져가도록 권장.
- 테스트는 코드를 작성한 후에 가능한 빨리 실행할 수 있어야함. -> 테스트를 먼저 만들면 코딩이 끝나자마자 테스트 실행 가능

## 테스트 코드 개선

- 테스트 실행할 때마다 반복되는 준비작업을 메소드에넣고, 테스트 메소드 실행전에 자동 실행시켜줌

### @Before

```
@Before
public void setUp(){
    ApplicationContext context =
            new AnnotationConfigApplicationContext(DaoFactory.class);
    
    this.dao = context.getBean("userDaoJdbc", UserDao.class);
}
```

### JUnit 작동 내부 순서

-
    1. 테스트 클래스에서 @Test가 붙은 public이고 void형이며 파라미터가 없는 테스트 메소드 모두 찾음
-
    2. 테스트 클래스의 오브젝트 하나 만듬 <<-- 주의 : 하나의 테스트 메소드만 사용하고 재사용x
-
    3. @Before가 붙은 메소드가 있으면 실행
-
    4. @Test가 붙은 메소드를 하나 호출하고 테스트 결과를 저장해둠
-
    5. @After가 붙은 메소드가 있으면 실행함
-
    6. 나머지 테스트 메소드에 대해 2~5 반복
-
    7. 모든 테스트 결과 종합하여 리턴

#### 테스트 클래스 오브젝트

- 각 테스트가 영향을 주지 않고 독립적으로 실행됨을 보장하기 위해 매번 새로운 오브젝트를 만들게 함.
- -> 인스턴스 변수 부담없이 사용가능.

### 픽스처

- 테스트 수행하는데 필요한 정보나 오브젝트를 픽스처(fixture)라고 함.
- UserDaoTest에서 dao가 대표적인 픽스처

## 테스트를 위한 애플리케이션 컨텍스트 관라

### 스프링 테스트 컨텍스트 프레임워크 적용

```
@Before
public void setUp(){
    ApplicationContext context =
            new AnnotationConfigApplicationContext(DaoFactory.class);
    //...
}
```

- 현재 테스트 메소드 만큼 애플리케이션 컨텍스트가 생성되고 있음.
- -> 애플리케이션 컨텍스트가 만들어 질때 모든 싱글톤 빈 오브젝트를 초기화함
- -> 성능도 부담스럽고, 빈이 할당한 리소스 깔끔하게 정리하지 않으면 문제 여지 있음
- -> 빈은 싱글톤으로 만들었기 때문에 상태를 갖지 않음. -> 애플리케이션 컨텍스트 한 번만 만들기
- -> 테스트 컨텍스트 프레임워크 사용

```
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="DaoFactory.java")
public class UserDaoTest {

    @Autowired
    private ApplicationContext context; // <<-- @Autowired는 빈을 찾아주는데, ApplicationContext도 찾아줬다..?
    //...
}
```

#### @RunWith

- @RunWith은 JUnit 프레임워크의 테스트 실행 방법을 확장할 때 사용하는 애노테이션
- SpringJUnit4ClasRunner라는 JUnit용 테스트 컨텍스트 프레임워크 확장 클래스 지정하면
- -> Junit이 테스트 진행하는 중에 테스트가 사용할 애플리케이션 컨텍스트 만들고 관리하는 작업을 진행해줌.

#### @ContextConfiguration

- 자동으로 만들어줄 애플리케이션 컨텍스트의 설정파일 위치를 지정.

### 테스트 클래스의 컨텍스트 공유

- 여러개의 테스트 클래스가 있는데 모두 같은 설정파일을 가진 애플리케이션 컨텍스트를 사용한다면
- -> 스프링은 테스트 클래스 사이에서도 애플리케이션 컨텍스트를 공유하게 해줌
- ex)

```java

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "DaoFactory.java")
public class UserDaoTest {
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "DaoFactory.java")
public class GroupDaoTest {
}
```

#### @Autowired

- @Autowired가 붙은 인스턴스 변수가 있으면, 테스트 컨텍스트 프레임워크는 변수타입과 일치하는 컨텍스트 내의 빈을 찾음

##### 중요 - @Autowired는 빈을 찾아주는데, ApplicationContext도 찾아줬다..?

- 스프링 **애플리케이션 컨텍스트는 초기화할 때 자기 자신도 빈으로 등록함** !!!
- -> 애플리케이션 컨텍스트에는 ApplicationContext 타입의 빈이 존재하는 것이고 DI도 가능한 것임
- -> @Autowired를 이용해 애플리케이션 컨텍스트가 갖고 있는 빈을 DI 받을 수 있따면
- -> 굳이 컨텍스트를 가져와 getBean()을 사용하는 것이 아니라, 아예 UserDao빈을 직접 DI 받을 수 있을 것.

##### @Autowired 시 같은 타입의 빈이 두 개 이상 있는 경우

- -> 변수 이름과 같은 이름의 빈이 있는지 확인
-

## DI와 테스트

- 효과적인 테스트를 위해서라도 DI를 적용해야만 함.
- -> DI는 테스트가 작은 단위의 대상에 대해 독립적으로 만들어지고 실행되게 하는데 중요한 역할을 함.

### 테스트 코드에 의한 DI

- DI는 스프링 컨테이너만 할 수 있는 작업 아님. 오브젝트 팩토리 예제에서 직접 DI했었음
- UserDao의 세터메소드도 테스트 코드에서도 얼마든지 호출해서 사용가능

#### UserDao가 사용할 DataSource 테스트

- applicationContext.xml에 정의된 DataSource 빈은 서버용이라고하면
- -> 테스트코드를 위한 DI를 이용해서 테스트용 DataSource를 주입

```
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "DaoFactory.java")
@DirtiesContext
public class UserDaoTest {
    @Before
    public void setUp() {
        DataSource dataSource = new SingleConnectionDataSource(
                "jdbc:mysql://localhost/testdb", "spring", "book", true);
        dao.setDataSource(dataSource);
        //...
    }
    //...
} 
```

- 이 방식은 매우 주의해서 사용해야함
- -> 이미 테스트 애플리케이션 컨텍스트에서 구성한 의존관계를 강제로 변경했기 때문
- -> 애플리케이션 컨텍스트의 구성이나 상태를 변경하지 않는 것이 원칙.-> 다른 모든 테스트의 영향
- -> @DirtiesContext

##### @DirtiesContext

- 스프링의 테스트 컨텍스트 프레임워크에게 해당 클래스의 테스트에서 애플리케이션 컨텍스트의 상태를 변경한다는 것을 알려줌
- -> 텍스트 컨텍스트는 이 @DirtiesContext 애노테이션이 붙은 테스트 클래스에는 애플리케이션 컨텍스트를 공유 허용하지 않음.
- -> **이곳에서 테스트 메소드를 수행하고 나면 매번 새로운 애플리케이션 컨텍스트가 생성**됨.
- cf) 메소드 레벨에서 사용 가능.

### 테스트를 위한 별도의 DI 설정

- -> 테스트에서 사용될 클래스가 빈으로 정의된 테스트 전용 설정파일을 따로 만들어두는 방법이 더 좋다 !

```
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "TestDaoFactory.java")
public class UserDaoTest {...}
```

### 컨테이너 없는 DI 테스트 <<-- 토비 추천

```
public class UserDaoTest{
    UserDao dao; //@Autowired가 없음
    
    @Before
    public void setUp(){
        dao = new UserDao(); //직접 생성
        DataSource dataSource = new SingleConnectionDataSoure(
            :jdbc://mysql://localhost/testdb", "spring", "book", true);
        dao.setDataSource(dataSource); // 컨테이너 사용X
    }    
```

#### DI를 이용한 테스트 방법 선택

- 항상 스프링 컨테이너 없이 테스트할 수 있는 방법을 우선적을 고려.
- -> 복잡한 의존관계를 갖고 있으면 스프링의 설정을 이용한 DI 이용. -> 테스트 전용 의존관계설정파일 따로 생성하기.
- -> 컨텍스트에서 DI받은 오브젝트 다시 수동 DI하는 경우 -> @DirtiesContext

## 학습테스트로 배우는 스프링

- 자신이 만들지 않은 코드에 대한 테스트 작성 -> 이를 "학습테스트"라고 함
- -> 학습 테스트는 테스트 대상보다는 테스트 코드 자체에 관심을 갖고 만들어야함.

### 학습테스트 AND 버그 테스트

# 3장 템플릿

- 변경이 잦은 성질으로부터, 일정 패턴으로 유지되는 부분을 독립시키는 방법

## 다시보는 초난감 DAO

- UserDao에 남은 문제점 -> 예외상황에 대한 처리

### 예외처리 기능을 갖춘 DAO

- DB커넥션이라는 제한적 리소스 공유하는 JDBC코드에 반드시 예외처리 필요함

#### JDBC 수정 기능의 예외처리 코드

```java
public class ex {
    public void deleteAll() throws SQLException {
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatme("delete from users");
        ps.executeUpdate(); // 예외 발생하면 메소드 실행이 중단됨
        ps.close(); // 자원처리를 못할 위험 존재
        c.close();
    }
}
```

- Connection과 PreparedStatement라는 두 개의 공유 리소스를 close하지 못하고 반환되지 않을 위험이 있음. -> 예외처리 필요.
- cf) Connection과 PreparedStatment는 보통 pool 방식으로 운영됨.

```java
import java.sql.SQLException;

public class ex {
    public void deleteAll() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = dataSource.getConnection();
            ps = c.prepareStatement("delete from users");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```

```java
public class ex {
    public int getCount() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            c = dataSource.getConnection();
            ps = c.prepareStatement("select count(*) from users");

            rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```

## 변하는 것과 변하지 않는 것

### JDBC try/catch/finally 코드의 문제점

- -> 리소스 반환 코드가 흩어져있다보니 빠뜨릴 가능성 존재
- -> 문제의 핵심은 변하지 않는 코드를 잘 분리해내는 작업.

### 분리와 재사용을 위한 디자인 패턴 적용

- 비슷한 기능의 메소드에서 동일하게 나타날 수 있는 고정된 부분과
- 비즈니스 로직에 따라 변할 수 있는 부분을 분리

#### 메소드 추출

```java
class ex {
    public void deleteAll() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = dataSource.getConnection();
//            ps = c.prepareStatement("delete from users");
            ps = makeStatemet(c); // 변하는 부분을 메소드로 추출하고 변하지 않는 부분에서 호출하도록 수정
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private PreparedStatement makeStatemet(Connection c) throws SQLException {
        PreparedStatement ps;
        ps = c.prepareStatement("delete from users");
        return ps;
    }
}
```

- 분리시킨 메소드를 재사용할 수 있어야하는데 반대로 됨.

#### 템플릿 메소드 패턴의 적용

- 템플릿 메소드 패턴은 상속을 통해 기능을 확장해서 사용하는 부분
- -> 변하지 않는 부분을 슈퍼클래스에 두고, 변하는 부분은 추상메소드로 정의해둬서, 서브클래스에서 오버라이드

```
abstract protected PreparedStatement makeStatemet(Connection c) throws SQLException;
```

```java
public class UserDaoDeleteAll extends UserDao {
    @Override
    protected PreparedStatement makeStatemet(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("delete from users");
        return ps;
    }
}
```

- **템플릿 메소드 패턴의 단점**은 로직마다 새로운 클래스를 만들어야한다는 점...!
- 또한 확장구조가 이미 클래스를 설계하는 시점에서 고정됨 -> 컴파일 시점에서 서브클래스와의 관계가 결정되어 유연성이 떨어짐.

#### 전략 패턴의 적용

- 개방폐쇄원칙을 잘 지키면서 템플릿메소드패턴 보다 유연하고 확장성이 뛰어난 것이
- -> **오브젝트를 아예 둘로 분리하고, 클래스레벨에서는 인터페이스를 통해서만 의존**하도록 만드는 **전략패턴**
  ![img](/img/img_2.png)
- **컨텍스트** : 변하지 않는 부분
- **전략** : 변하는 부분에 대한 인터페이스
- 좌측에 있는 Context의 contextMethod()에서 일정한 구조를 가지고 동작하다가
- -> 특정 확장 기능은 Strategy 인터페이스를 통해 외부의 독립된 전략 클래스에 위임.
- deleteAll()에서 변하지 않는 부분이 이 contextMethod()가 됨

```java
public interface StatementStrategy {
    PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
```

```java
public class DeleteAllStatement implements StatementStrategy {
    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("delete from users");
        return ps;
    }
}
```

```
public void deleteAll() throws SQLException{
//... 
  try {
            c = dataSource.getConnection();
            StatementStrategy strategy = new DeleteAllStatement();
            ps = strategy.makePreparedStatement(c);
            
            ps.executeUpdate();
        } catch (SQLException e) {
//...
}
```

- -> 하지만 전략패턴은 필요에 따라 컨텍스트는 유지되면서 전략을 바꿔쓸 수 있느 ㄴ것인데,
- -> 위처럼 구체적인 전략 클래스인 DeleteAllStatement를 사용하도록 고정되어 있는 것은 이상함.
- -> 컨텍스트가 구체적인 구현 클래스를 직접 알고 있다는건, 전략패턴도 OCP원칙도 맞지 않음

#### DI 적용을 위한 클라이언트 / 컨텍스트 분리

- 전략 패턴에 따르면 Context가 어떤 전략을 사용할 것인가는
- -> Context를 사용하는 앞단의 Client가 결정하는 게 일반적.
- Client가 구체적인 전략의 하나를 선택하고 오브젝트로 만들어서 Context에 전달
- -> Context는 전달받은 그 Strategy를 구현 클래스의 오브젝트에 사용.
  ![img](/img/img_3.png)
- -> 중요한 것은 클라이언트 코드인 StatementStrategy를 만드는 부분을 독립 시켜야함 !

```
StatementStrategy strategy = new DeleteAllStatement();
```

```java
public class ex {
    public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = dataSource.getConnection();
            ps = stmt.makePreparedStatement(c);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```

- -> 클라이언트로부터 StatementStrategy 타입의 전략 오브젝트를 제공받고
- -> JDBC try/catch/finally 구조로 만들어진 컨텍스트 내에서 작업을 수행함.
- -> 제공받은 전략 오브젝트는 PreparedStatement 생성이 필요한 시점에 호출해서 사용

##### 메소드가 전략 패턴의 클라이언트가 됨 !

- 컨텍스트를 별도의 메소드로 분리했으므로 deleteAll() 메소드가 클라이언트가 됨
- delteAll()은 전략 오브젝트를 만들고 컨텍스트를 호출하는 책임을 지고 있음.

```java
class ex {
    public void deleteAll() throws SQLException { // 전략패턴의 클라이언트가 된 메소드
        StatementStrategy st = new DeleteAllStatement(); //선정한 전략 클래스의 오브젝트 생성
        jdbcContextWithStatementStrategy(st); //컨텍스트 호출, 전략 오브젝트 전달. 
    }
} 
```

- -> 클라이언트가 사용할 전략을 정해서 컨텍스트로 전달하는 면에서 **DI 구조**
- -> 마이크로 DI(=수동DI) -> 전랙패턴 구조에 따라 클라이언트가 오브젝트 팩토리의 책임을 지고 있음.

## JDBC 전략 패턴의 최적화

### 전략 클래스의 추가 정보

- 매개변수가 필요한 전략인 경우

```java
public class AddStatement implements StatementStrategy {
    User user;

    public AddStatement(User user) {
        this.user = user;
    }

    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        PreparedStatement ps =
                c.prepareStatement("insert into users(id, name, password) values(?,?,?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        return ps;
    }
}
```

### 전략과 클라이언트의 동거

- 현재 구조의 문제점
- DAO 메소드마다 새로운 StatementStrategy 구현 클래스르 만들어야함 -> 클래스 너무 많아짐.
- 전략에 전달해야할 부가적인 정보가 있을 경우, 오브젝트를 전달받는 생성자와 이를 저장해둘 인스턴스 변수를 번거롭게 만들어야함.
- -> 문제 해결 -> 로컬 클래스

- cf) 중첩 클래스의 종류
- 중첩 클래스 : 다른 클래스 내부에 정의되는 클래스를 중첩 클래스(nested class)라고 함
- 중첩 클래스 -> "스태틱 클래스"와 "내부 클래스"로 구분됨
- 스태틱 클래스 : 독립적으로 오브젝트로 만들어질 수 있음
- 내부클래스 : 자신이 정의된 클래스의 오브젝트 안에서만 만들어질 수 있음.
- 내부클래스 -> 범위에 따라 세가지로 구분됨 (멤버 내부 클래스, 로컬 클래스, 익명 내부 클래스)
- 멤버 내부 클래스 : 멤버 필드처럼 오브젝트 레벨에 정의 됨
- 로컬 클래스 : 메소드 레벨에 저으이됨
- 익명 내부 클래스 : 이름을 갖지 않으며 범위는 선언된 위치에 따라 다름.

#### 로컬 클래스  (메소드 안에 선언된 내부 클래스)

- 클래스파일이 많아지는 문제 해결 방법 1
- -> StatementStrategy 전략 클래스를 매번 독립된 파일로 만들지 말고 UserDao 클래스 안에, 메소드 내부의 내부 클래스로 정의

```java
class ex {
    public void add(User user) throws SQLException {
        public class AddStatement implements StatementStrategy {
            User user;

            public AddStatement(User user) {
                this.user = user;
            }

            @Override
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps =
                        c.prepareStatement("insert into users(id, name, password) values(?,?,?)");
                ps.setString(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getPassword());

                return ps;
            }
        }
        StatmentStrategy st = new AddStatement(user);
        jbdcContextWithStatementStrategy(st);
    }
}
```

- -> 로컬 클래스 사용하면, 자신이 정의된 메소드의 로컬 변수에 직접 접근 가능 !
- **메소드 파리미터도 일종의 로컬 변수 이므로** add() 메소드의 user변수를 AddStatement에서 직접 사용할 수 있음
- -> 다만 내부클래스에서 외부의 변수를 사용할 때는 외부 변수 반드시 final로 선언해야함 !

```java
class ex {
    public void add(final User user) throws SQLException { //로컬 변수 user
        class AddStatement implements StatementStrategy {
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps =
                        c.prepareStatement("insert into users(id, name, password) values(?,?,?)");
                ps.setString(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getPassword()); //로컬 클래스에서 외부 메소드 로컬 변수에 직접 접근 가능 !!! (final일 경우)
                return ps;
            }
        }
        StatementStrategy st = new AddStatement();
        jdbcContextWithStatementStrategy(st);
    }
}
```

#### 익명 내부 클래스

- AddStatement 클래스는 add() 메소드에서만 사용할 용도로 만들어졌음 -> 간결하게 클래스 이름까지 제거
- cf) 익명 내부 클래스 : 클래스 선언과 오브젝트 생성이 결합된 형태. 상속또는 구현할 대상을 생성자 대신 사용해서 선언. 재사용필요 없을 경우, 상속 또는 구현할 타입으로만 사용할 경우 유용.
- > 선언가 동시에 오브젝트로 생성, 이름이 없기 때문에 자신의 클래스 타입 가질 수 없음. -> 상속 또는 구현 대상 타입의 변수에만 저장 가능

```
new 인터페이스 이름() { 클래스 본문 };
```

- AddStatement를 익명 내부 클래스로 전환

```java
class ex {
    public void add(final User user) throws SQLException { //로컬 변수 user
        StatementStrategy st = new StatementStrategy() { //익명 내부 클래스 !!! -> 구현하는 인터페이스를 생성자처럼 이용해서 오브젝트 만듬
            @Override
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps =
                        c.prepareStatement("insert into users(id, name, password) values(?,?,?)");
                ps.setString(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getPassword()); //로컬 클래스에서 외부 메소드 로컬 변수에 직접 접근 가능 !!! (final일 경우)
                return ps;
            }
        };
        StatementStrategy st = new AddStatement();
        jdbcContextWithStatementStrategy(st);
    }
}
```

- -> 만들어진 익명 내부 클래스의 오브젝트는 딱 한 번만 사용되니, 변수에 담아두지 않고 파라미터에서 바로 생성하는 것이 낫다.

```java
class ex {
    public void add(final User user) throws SQLException { //로컬 변수 user
        jdbcContextWithStatementStrategy(
                new StatementStrategy() { //익명 내부 클래스 !!! -> 구현하는 인터페이스를 생성자처럼 이용해서 오브젝트 만듬
                    @Override
                    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                        PreparedStatement ps =
                                c.prepareStatement("insert into users(id, name, password) values(?,?,?)");
                        ps.setString(1, user.getId());
                        ps.setString(2, user.getName());
                        ps.setString(3, user.getPassword()); //로컬 클래스에서 외부 메소드 로컬 변수에 직접 접근 가능 !!! (final일 경우)
                        return ps;
                    }
                });
    }
}
```

## 3.4 컨텍스트와 DI

### JdbcContext의 분리

- jdbc의 일반적인 작업 흐름을 담고 있는 jdbcContextWithStatementStrategy()는 다른 DAO에서도 사용 가능
- -> UserDao 클래스 밖을 독립시켜서 모든 DAO가 사용할 수 있게 해보기

#### 클래스 분리 -> 템플릿/콜백 패턴

- 분리해서 만들 클래스 이름은 JbdcContext -> 전략 패턴에서 컨텍스트에 해당되는 부분 이므로
- -> 익명내부클래스를 사용한 전략 패턴 ==> **템플릿/콜백 패턴**

```java
public class JdbcContext {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void workWithStatementStrategy(StatementStrategy stmt) throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = this.dataSource.getConnection();
            ps = stmt.makePreparedStatement(c);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```

```java
// 템플릿/콜백 패턴
class ex {
    public void deleteAll() throws SQLException { // 전략패턴의 클라이언트가 된 메소드
        jdbcContext.workWithStatementStrategy( // 컨텍스트에 전략 전달...
                new StatementStrategy() { //익명 내부 클래스 !!! -> 구현하는 인터페이스를 생성자처럼 이용해서 오브젝트 만듬
                    @Override
                    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                        return c.prepareStatement("delete from users");
                    }
                }
        );
    }
}
```

#### 빈 의존관계 변경

- UserDao는 이제 JdbcContext에 의존하고 있음
- JdbcContext는 인터페이스인 DataSource와 달리 구체 클래스
- 스프링 DI는 기본적으로 인터페이스를 사이에 두고 의존 클래스를 바꿔서 사용 하는 것이지만,,
- -> 이 경우는 jdbcContext는 그 자체로 독립적인 JDBC 컨텍스트를 제공해주는 서비스 오브젝트로서 의미가 있을 뿐이고, 구현 방법이 바뀔 가능성은 없음
- -> 인터페이스를 사이에 두지 않고 DI를 적용하는 특별한 구조.
  ![img](/img/img_4.png)

### JdbcContext의 특별한 DI

- 인터페이스를 거치지 않고 바로 클래스를 사용하여, 런타임시 의존 오브젝트의 구현 클래스를 변경할 수 없게 되었음

#### 스프링 빈으로 DI

- 항상 인터페이스를 사용해서 주입해야하는 것은 아님!
- -> 스프링의 DI는 넓게 보자면 객체의 생성과 관계 설정에 대한 오브젝트에서 제거하고 외부로 위임했다는 IoC 개념을 포괄
- -> 인터페이스를 사용하지 않더라도 JdbcContext를 스프링을 이용해 UserDao 객체에서 사용하게 주입했다는 건 DI의 기본을 따르고 있다고 볼 수 있는 것!
- 인터페이스 사용하지 않았더라도 **DI 구조 해야하는 이유**
- -> JdbcContext 싱글톤으로 사용해야하기 때문 -> dataSource라는 인스턴스 변수가 있지만, 읽기 전용이므로 싱글턴에 문제 없음
- -> JdbcContext가 DI를 통해 다른 빈에 의존하고 있기 때문 -> DI 받기 위해선 양쪽 스프링 빈이어야함.

##### 인터페이스 사용 여부?

- -> 인터페이스가 없다는 것은 UserDao와 JdbcContext가 매우 긴밀한 관계를 가지고 강하게 결합되어 있기 때문
- -> 다른 db 접근 방식을 사용해야한다면 클래스 자체가 사라져야하고
- -> 테스트에서도 다른 구현으로 대체해서 사용할 이유가 없음.
- -> 이런경우 강한 결합 관계를 유지하면서 싱글톤과, DI를 위한 스프링 빈으로 등록하여 DI되도록 함.

### 코드를 이용하는 수동 DI -> 이렇게 하는 것에 대한 장점은..? -> 긴밀한 클래스끼리 굳 빈으로 분리하지 않고 싶을 떄...

- -> 빈 등록하지 않고 DAO마다 하나의 JdbcContext 오브젝트를 갖게 할 수도 있음.
- -> JdbcContext는 내부 상태 정보가 없기 때문에 많이 만들어져도 메모리에 부담 거의 없음.
- -> 자주 만들어졌다가 제거되는 것도 아니기에 GC 부담도 없음
- 빈이 아니라면 JdbcContext의 제어권은 UserDao가 갖는 것이 적당함.
- -> 그렇다면 DI 사용 두번쨰 이유인, **다른 빈에 대한 의존은 어떻게 처리 ???**
- -> JdbcContext의 제어권을 갖고 생성과 관리를 담당하는 UserDao에세 DI를 맡기기...
  ![img](/img/img_5.png)

```java
class ex {
    public void setDataSource(DataSource dataSource) {
        this.jdbcContext = new JdbcContext();
        this.jdbcContext.setDataSource(dataSource);
        this.dataSource = dataSource; // 아직 JdbcContext를 적용하지 않은 메소드를 위해 저장해둠
    }
}
```

- -> 이렇게 한 오브젝트의 수정자 메소드에서 다른 오브젝트를 초기화 하고 코드를 이용해 DI 하는 것은 스프링에서 종종 사용되는 기법.

### 정리 - 결합이 강한 클래스끼리 경우의 DI 방법 2가지.

- 인터페이스 사용하지 않고 다른 클래스와 밀접한 클래스 DI 방법 두가지
- -> 1. 인터페이스 사이에 끼지 않고도 빈으로 직접 DI -> DI 원칙에 부합하지 않게 구체 클래스와의 관계가 설정에 직접 노출 단점
- -> 2. 빈으로 두지 않고 그냥 생성자 수동 DI하여 로직 추가 -> 싱글톤X, 수동 DI 추가 코드 작성

# 3.5 템플릿과 콜백

- 지금까지 UserDao와 StatmentStrategy, JdbcContext를 이용해 만든 코드는 일종의 전략 패턴이 적용된 것.
- -> "전략 패턴의 기본 구조에 익명 내부 클래스"를 활용했음.
- --> 이런 패턴을 스프링에서는 **템플릿/콜백 패턴**이라고 부름
- -> 전략 패턴의 컨텍스트를 **템플릿**이라고 부르고
- -> 익명내부 클래스로 만들어지는 오브젝트(전략)을 **콜백**이라고 부름.

- cf)
- 템플릿 : 어떤 목적을 위해 미리 만들어둔 모양이 있는 틀
- -> 템플릿 메소드 패턴은 고정된 틀의 로직을 가지고 템플릿 메소드를 슈퍼클래스에 두고, 바뀌는 부분을 서브클래스의 메소드에 두는 구조.
- 콜백 : 다른 오브젝트로 전달되어서 **실행될 메소드를 보유한 오브젝트**.
- -> 파라미터로 전달되지만 값을 참조하기 위한 것이 아니라, 특정 로직을 담은 메소드를 실행시키기 위해 사용.
- -> **자바에선 메소드 자체를 파라미터로 전달할 방법이 없기 떄문**에 사용할 메소드를 갖고 있는 오브젝트를 전달 -> 펑서녈 오브젝트라고도 함

## 템플릿/콜백의 동작원리

### 템플릿/콜백의 특징

- 여러개의 메소드를 가진 인터페이스를 사용하는 일반적인 전략패턴과 달리, 템플릿/콜백 패턴의 콜백은 보통 단일 메소드 인터페이스를 사용.
- -> 템플릿(컨텍스트)의 작업 흐름 중 특정 기능을 위해 한 번 호출되는 경우가 일반적이기 떄문
- -> 하나의 템플릿에 여러 전략을 사용해야한다면 하나 이상의 콜백 오브젝트를 사용할 수도 있음
- -> 콜백은 일반적으로**하나의 메소드를 가진 인터페이스를 구현한 익명 내부 클래스**

```java
public interface StatementStrategy { //콜백 패턴을 위한 인터페이스 -> 보통 단일 메소드를 가진 인터페이스
    PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
```

#### 콜백 인터페이스 메소드의 파라미터

- 콜백 인터페이스 메소드에는 보통 파라미터가 있음. (위의 Connection c)
- 이 파라미터는 템플릿의 작업 흐름 중에 만들어지는 컨텍스트의 정보를 전달 받을 때 사용됨.
- 일반적인 템플릿/콜백 패턴의 일반적인 흐름
  ![img](/img/img_6.png)
- -> 클라이언트가 템플릿 메소드를 호출하면서 콜백 오브젝트를 전달하는 것은 메소드 레벨에서 일어나는 DI
- -> 클라이언트와 콜백이 강하게 결합된 DI

## 편리한 콜백의 재활용

- 템플릿/콜백 방식의 아쉬운 점 -> DAO 메소드에서 매번 익명 내부 클래스를 사용하기 때문에
- -> 상대적으로 코드를 작성하고 읽기가 조금 불편하다는 점

### 콜백의 분리와 재활용

- 복합한 익명 내부 클래스의 사용을 최소화하는 방법
- 분리를 통해 재사용 가능한 코드를 찾아낼 수 있다면 익명 내부 클래스 사용 코드를 간결하게 만들 수 있음.

```java
class ex {
    public void deleteAll() throws SQLException { // 전략패턴의 클라이언트가 된 메소드
        executeSql("delete from users");
    }

    private void executeSql(final String query) throws SQLException {
        this.jdbcContext.workWithStatementStrategy(
                new StatementStrategy() {
                    @Override
                    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                        return c.prepareStatement(query);
                    }
                }
        );
    }
}
```

- -> 바뀌는 부분인 SQL 문장만 파라미터로 받아서 사용 -> 파라미터를 final로 선언해서 익명내부클래스인 콜백 안에서 사용하도록 하는 것 주의

### 콜백과 템플릿의 결합

- 재사용 가능한 콜백을 담고 있는 메소드라면 DAO가 공유할 수 있는 템플릿 클래스 안으로 옮기기
- -> 엄밀히 말하면 템플릿은 JdbcContext 클래스가 아니라, workWithStatementStrategy() 메소드이므로 JdbcContext로 exeuteSql() 옮겨도 문제 없음
  ![img](/img/img_7.png)

### 테스트와 try/catch/finally

```java
public class Calculator {
    public Integer calcSum(String filePath) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath)); // 한 줄씩 읽기 편하게 BufferedReader
            Integer sum = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                sum += Integer.valueOf(line);
            }
            return sum;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) { // BufferedReader 오브젝트가 생성되기 전에 예외가 발생할 수도 있으므로 반드시 null 체크
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
```

### 중복의 제거와 템플릿/콜백 설계

- 템플릿/콜백 패턴 적용
- -> 템플릿에 담을 반복되는 작업흐름 살피기
- -> 템플릿이 콜백에게 전달해줄 내부의 정보는 무엇?
- -> 콜백이 템플릿에게 돌려줄 내용은 무엇?
- -> 템플릿이 클라이언트에게 돌려줄 내용은 무엇?
- -> 클라이언트에서 템플릿을 사용하면서, 익명내부클래스로 콜백을 전달하는 구조 기억하기!

```java
public class Calculator {
    // ..
    // 템플릿 메소드
    public Integer fileReadTemplate(String filePath, BufferedReaderCallback callback) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            int ret = callback.doSomethingWithReader(br); // 템플릿에서 만든 컨텍스트 정보 콜백에 전달
            return ret;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println();
                }
            }
        }
    }
}
```

```java
public interface BufferedReaderCallback {
    // 전략패턴의 콜백 인터페이스
    Integer doSomethingWithReader(BufferedReader br) throws IOException;
}
```

```java
public class Calculator {
    // 클라이언트 메소드
    public Integer calcSum(String filePath) throws IOException {
        BufferedReaderCallback sumCallback = new BufferedReaderCallback() { // 콜백 오브젝트
            @Override
            public Integer doSomethingWithReader(BufferedReader br) throws IOException {
                Integer sum = 0;
                String line = null;
                while ((line = br.readLine()) != null) {
                    sum += Integer.valueOf(line);
                }
                return sum;
            }
        };

        return fileReadTemplate(filePath, sumCallback); // 템플릿에 콜백 오브젝트 전달 !
    }
    //...
}
```

### 템플릿/콜백의 재설계

- 콜백의 공통점 발견 -> 인터페이스 정의

```java
// 콜백 인터페이스
public interface LineCallback {
    Integer doSomethingWithLine(String line, Integer value);
}
```

```java
public class Calculator {
    //..
    // 템플릿 메소드
    public Integer lineReadTemplate(String filePath, LineCallback callback, int initVal) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            Integer res = initVal;
            String line = null;
            while ((line = br.readLine()) != null) {
                res = callback.doSomethingWithLine(line, res);
            }
            return res;
        } catch (IOException e) {
            //..
        } finally {
            //..
        }
    }
}
```

```java
public class Calculator {
    // 클라이언트에서 콜백 오브젝트 생성.
    public Integer calcSum(String filePath) throws IOException {
        LineCallback sumCallback = new LineCallback() {
            @Override
            public Integer doSomethingWithLine(String line, Integer value) {
                return value + Integer.valueOf(line);
            }
        };
        return lineReadTemplate(filePath, sumCallback, 0);
    }

    // 클라이언트에서 콜백 오브젝트 생성.
    public Integer calMultiply(String filPath) throws IOException {
        LineCallback multiplyCallback = new LineCallback() {
            @Override
            public Integer doSomethingWithLine(String line, Integer value) {
                return value * Integer.valueOf(line);
            }
        };

        return lineReadTemplate(filPath, multiplyCallback, 1);
    }
    //..
}    
```

### 제네릭스를 이용한 콜백 인터페이스

- 지금까지 사용한 LineCallback과 lineTemplate()은 템플릿과 콜백이 만들어내는 결과가 Integer타입으로 고정
- -> 결과의 타입을 다양하게 가져가고 싶으면, 타입 파라미터라는 개념을 도입한 제네릭스를 이용하면 됨!
- -> 제네릭스를 이용하면 다양한 오브젝트 타입을 지원하는 인터페이스나 메소드를 정의할 수 있음.

```java
public interface LineCallback<T> {
    T doSomethingWithLine(String line, T value);
}
```

- 템플릿인 lineReadTemplate() 메소드도 타입 파라미터를 사용해 제네릭 메소드로 만들어줌

```java
class ex {
    public <T> T lineReadTemplate(String filePath, LineCallback<T> callback, T initVal) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            T res = initVal;
            String line = null;
            while ((line = br.readLine()) != null) {
                res = callback.doSomethingWithLine(line, res);
            }
            return res;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println();
                }
            }
        }
    }
}
```

- -> 콜백의 타입 파라미터와 초기값인 initVal타입, 템플릿 결과 값 타입 동일하게 선언해줘야함.

```java
class ex {
    public String concatenate(String filepath) throws IOException {
        LineCallback<String> concatenateCallback =
                new LineCallback<String>() {
                    @Override
                    public String doSomethingWithLine(String line, String value) {
                        return value + line;
                    }
                };
        // 템플릿 메소드 lineReadTemplate의 T는 모두 스트링이 된다.
        return lineReadTemplate(filepath, concatenateCallback, "");
    }

}
```

## 스프링의 JdbcTemplate

- 스프링이 제공하는 템플릿/콜백 기술 살펴보기
- JDBC코드용 기본 템플릿은 **JdbcTemplate**

```java
public class UserDao {
    //  private JdbcContext jdbcContext;
    public JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
//        this.jdbcContext = new JdbcContext();
//        this.jdbcContext.setDataSource(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    //...
}
```

### update()

- deleteAll()에 적용했던 콜백은 StatementStrategy 인터페이스의 makePreparedStatement() 메소드
- 이에 대응되는 JdbcTemplate의 콜백은 PreparedStatementCreator 인터페이스의 createPreparedStatement() 메소드
- -> 템플릿으로부터 Conntection을 받아서 PreparedStatement를 만들어 준다는 면에서 구조 동일
- -> PreparedStatementCreator 타입의 콜백을 받아서 사용하는 JdbcTemplate의 템플릿 메소드는 update()

### queryForInt()

- SQL 쿼리를 실행하고 ResultSet을 통해 결과를 가져오는 코드에 쓸 수 있는 템플릿음
- PreparedStatementCreator 콜백과 ResultSetExtractor 콜백을 파라미터로 받는 query() 메소드

```java
class ex {
    public int getCount() {
        return this.jdbcTemplate.query(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                return con.prepareStatement("select count(*) from users");
            }
        }, new ResultSetExtractor<Integer>() {
            public Integer extractData(ResultSet rs) throws SQLException {
                rs.next();
                return rs.getInt(1);

            }
        });
    }

    public int getCount() { //queryForInt 사용
        return this.jdbcTemplate.queryForInt("select count(*) from users");
    }
}
```

### queryForObject()

- ResultSet의 결과를 User 오브젝트를 만들어 프로퍼티에 넣어주기
- 위에서 제네릭 설정 해주었던 ResultSetExtract 콜백 대신 RowMapper 콜백을 사용.
- -> 차이점은 ResultSetExtractor는 한 번 전달받아 알아서 추출작업을 모두 진행하고 최종 결과만 리턴
- -> RowMapper는 ResultSet 로우 하나를 매핑하기 때문에 여러번 호출 가능.

```java
class ex {
    public User get(String id) {
        return this.jdbcTemplate.queryForObject("select * from users where id = ?",
                new Object[]{id}, // SQL에 바인딩할 파라미터 값, 가변인자 대신 배열을 사용.
                new RowMapper<User>() {
                    @Override
                    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                        User user = new User();
                        user.setId(rs.getString("id"));
                        user.setName(rs.getString("name"));
                        user.setPassword(rs.getString("password"));
                        return user;
                    }
                });
    }
}
```

- -> queryForOjbect()는 SQL 실행해서 받은 로우의 개수가 하나가 아니라면 예외를 던짐.

### query()

#### 기능 정의와 테스트 작성

- 위의 queryForObject()는 쿼리의 결과가 로우 하나일 때 사용
- query()는 여러 개의 로우가 결과로 나오는 일반적인 경우에 쓸 수 있음.
- query()는 제네릭 메소드로 타입은 파라미터로 넘기는 RowMapper<T> 콜백 오브젝트에서 결정됨.

```java
class ex {
    public List<User> getAll() {
        return this.jdbcTemplate.query("select * from users order by id",
                new RowMapper<User>() {
                    @Override
                    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                        User user = new User();
                        user.setId(rs.getString("id"));
                        user.setName(rs.getString("name"));
                        user.setPassword(rs.getString("password"));
                        return user;
                    }
                });
    }
}
```

- 첫 번째 파라미터에는 실행할 SQL 쿼리를 넣음
- 바인딩할 파라미터가 있다면 두 번째 파라미터에 추가 가능
- 마지막 파라미터는 RowMapper 콜백 -> 가져오는 로우 만큼 호출됨.
- -> 결과가 없을 경우 크기가 0인 List<T> 오브젝트 반환

### 테스트 보완 - 네거티브 테스트

- 예외 상황에 대한 테스트 -> 예외적인 조건에 대해 먼저 테스트를 만드는 습관이 좋음
- ex) get()이라면 Id가 없을 때는 어떻게 되는지, getAll()이라면 결과가 하나도 없는 경우에는 어떻게 되는지 ..
- query()의 결과와 상관없이 getAll() 메소드의 예외 상황에 대한 테스트 반드시 필요.

## 재사용 가능한 콜백의 분리

### DI를 위한 코드정리

```java
public class UserDao {
    public JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) { // DI
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    //..
}
```

- -> 수정자 메소드에거 이렇게 다른 오브젝트를 생성하는 경우 종종 있음.

### 중복 제거

- RowMapper 콜백의 내용이 중복됨
- -> 매번 RowMapper 오브젝트를 새로 만들어야할지 생각하기
- -> RowMapper 콜백 오브젝트에는 상태 정보가 없으므로 -> 멀티스레드에서 동시에 사용해도 문제 없음
- -> 하나만 만들어서 공유하기

```java
public class UserDao {
    private RowMapper<User> userMapper =
            new RowMapper<User>() {
                @Override
                public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                    User user = new User();
                    user.setId(rs.getString("id"));
                    user.setName(rs.getString("name"));
                    user.setPassword(rs.getString("password"));
                    return user;
                }
            };

    //...
    public User get(String id) {
        return this.jdbcTemplate.queryForObject("select * from users where id = ?",
                new Object[]{id},
                this.userMapper);
    }

    public List<User> getAll() {
        return this.jdbcTemplate.query("select * from users order by id",
                this.userMapper);
    }
}
```

### 템플릿/콜백 패턴과 UserDao

- UserDao에는 User정보를 DB에 넣거나 가져오거나 조작하는 방법에 대한 핵심적인 로직만 담겨 있음
- -> 응집도가 높음
- 반면, JDBC API 사용 방식, 예외처리, 리소스 반납, DB 연결등의 책임과 관심은 JdbcTemplate에게 이씅ㅁ
- -> 결합도 낮음
- -> 다만, JdbcTemplate이라는 템플릿 클래스는 직접 이용한다는 면에서 특정 템플릿/콜백 구현에 강한 결합을 갖고 있음.
- -> 더 낮은 결합도를 유지하고 싶다면 JdbcTemplate을 독립적인 빈으로 등록하고, JdbcTemplate이 구현하고 있는 JdbcOperations인터페이스를 통해 DI 받아 사용하도록 만들어도 됨
- -> JdbcTemplate은 DAO안에서 직접 만들어 사용하는게 스프링 관례이긴 하나, 독립된 싱글톤으로 등록하고 DI 받아 인터페이스를 통해 사용 가능.
- 스프링에서 클래스 이름이 Template으로 끝나거나 인터페이스 이름이 Callback으로 끝난다면 템프릿/콜백이 적용된 것

## 정리

### 전략패턴 적용

- 일정한 작업 흐름이 반복되면서 그중 일부 기능만 바뀌는 코드가 존재한다면 **전략 패턴**을 적용. 바뀌지 않는 부분은 컨텍스트로, 바뀌는 부분은 전략으로 만들고 인터페이스를 통해 유연하게 전략을 변경
- 여러 전략을 다이나믹하게 구성하고 사용해야한다면 컨텍스트를 이용하는 **클라이언트 메소드에서 직접 전략을 정의하고 제공**하게 만듬
- **클라이언트 메소드 안에 익명 내부 클래스를 사용하여 전략 오브젝트를 구현**하면 코드 간결, 메소드 정보 직접 사용 가능하여 편리
- 컨텍스트가 하나 이상의 클라이언트 오브젝트에서 사용된다면 클래스를 분리하여 공유
- 컨텍스트는 별도의 빈으로 등록해서 DI 받거나 클라이언트 클래스에서 직접 사용. 클래스 내부에서 컨텍스트 사용할 떄, 컨텍스트가 의존하는 외부 오브젝트 있다면 코드 이용해 직접 DI 해줄 수 있음
- 단일 전략 메소드를 갖는 전략 패턴이면서, 익명 내부 클래스를 사용하여 매번 전략을 새로 만들고, 컨텍스트 호출과 동시에 전략 DI를 수행하는 방식을 템플릿/콜랙이라고 함
- 콜백의 코드에도 일정한 패턴이 반복된다면 콜백을 템플릿에 넣고 재활용
- 템플릿과 콜백의 타입이 다양하게 바뀔 수 있다면 제네릭스 이용
- 템플릿은 한 번에 하나 이상의 콜백을 사용할 수도 있고 하나의 콜백을 여러번 호출할 수도 있음
- 템플릿/콜백 설계 시 템플릿과 콜백 사이 주고받는 정보에 관심을 둬야함

# 예외

- 예외 처리하는 베스트 프랙티스 살펴보기

## 사라진 SQLException

- JdbcContext (커스텀 클래스)-> JdbcTemplate으로 바꾸면서 예외 처리 부분이 생략됨

```java
import java.sql.SQLException;

class ex {
    public void deleteAll() throws SQLException {
        this.jdbcContext.executeSql("delete from users");
    }

    public void deleteAll() { //예외 사라짐 !
        this.jdbcTemplate.update("delete from users");
    }
}
```

- SQLException은 JDBC API의 메소드들이 던지는 것이므로 당연히 있어야함.
- 로그를 남기기위해 catch 했다고 해도 다시 JDBC 템플릿 메소드 밖으로 던져서 예외 상황이 발생했다는 사실을 알려야함.
- -> JdbcTemplate 적용한 코드의 SQLException은 어디로 간 것일까..?

## 초난감 예외처리

- 잘못된 예외처리 코드 예시

### 예외 블랙홀

```
try{
//...    
}catch(SQLException e){ // 예외를 잡고 아무 처리 안하고 있음 ..!
}
```

- -> 원치않는 예외가 발생하는 것보다 훨씬 더 나쁜 일..

```
}catch(SQLException e){
    System.out.println(e);
}
```

```
}catch(SQLException e){
    e.printStackTrace();
}
```

- -> 위의 코드도 문제가 있음
- -> 예외는 반드시 처리가 되어야함. 메시지 출력은 예외를 처리한 것이 아님.
- **예외처리 핵심 원칙** : 모든 예외는 적절하게 복구되든지 아니면 작업을 중단시키고 운영자 또는 개발자에게 분명하게 통보돼야 함.
- 예외릘 잡아서 뭔가 조치를 취할 방법이 없다면 잡지 말아야함.
- -> 메소드에 throws SQLException을 선언해서 메소드 밖으로 던지고 자신을 호출한 코드에 예외처리 책임을 전가

### 무의미하고 무책임한 throws

- 아래와 같이 메소드 선언에 throws Exception을 기계적으로 붙이는 것도 매우 좋지 않음

```java
class ex {
    public void method1() throws Exception {
        method2();
    }

    public void method2() throws Exception {
        method3();
    }

    public void method3() throws Exception {
        //..
    }
}
```

- 무책임하게 throws Exception을 사용하는 메소드를 사용하는 메소드도 throws Exception을 따라 붙일 수 밖에 없게 됨..

## 예외의 종류와 특징

- 자바에서 throw를 통해 발생시킬 수 있는 예외는 크게 세 가지
- 1 **Error**
- 2 **Exception과 체크 예외**
- 3 **RuntimeException과 언체크/런타임 예외**

### Error (에러)

- java.lang.Error 클래스의 서브 클래스들.
- **에러는 시스템에 뭔가 비정상적인 상황이 발생**했을 경우세 사용됨
- **주로 자바 VM에서 발생시키는 것**이고 애플리케이션 코드에서 잡으려고 하면 안됨
- -> OutOfMemoryError나 ThreadDeath 같은 에러는 catch 블록으로 잡아봤자 아무런 대응 방법이 없기 때문
- -> 따라서 시스템 레벨에서 특별한 작업을 하는 게 아니라면 애플리케이션에서는 이런 에러에 대한 처리는 신경 쓰지 않아도 됨 !

### Exception과 체크 예외 (일반적으로 예외는 체크 예외 - RuntimException상속x)

- 개발자들이 만든 애플리케이션 코드의 작업 중에 예외상황이 발생했을 경우에 사용됨.
- **Exception 클래스는 다시 체크 예외와 언체크 예외로 구분**됨
- **체크예외**는 Exception 클래스의 서브클래스이면서 RuntimeException 클래스를 상속하지 않은 것
- **언체크예외**는 RuntimeException을 상속한 클래스들임
- RuntimeException은 Exception의 서브 클래스이긴 하지만, 자바는 RuntimeException과 그 서브클래스는 특별하게 다룸
  ![](img/img_8.png)
- **일반적으로 예외**라고하면 RuntimeException을 상속하지 않은 **체크 예외라고 생각**해도 됨
- 체크 예외가 발생할 수 있는 메소드를 사용할 경우 반드시 예외를 처리하는 코드를 함께 작성해야함 !
- cf)자바언어와 JDK초기 설계자들은 체크예외를 발생가능한 예외에 모두 적용하려고 했던 것 같음
- -> 그래서 IOException이나 SQLException을 비롯해서 예외적인 상황에서 던져질 가능성이 있는 것들은 대부분 체크 예외로 만들어져있음.
- 최근 자바 표준 스펙 API들은 예상가능한 예외상황을 다루는 예외를 체크 예외로 만들지 않는 경향이 있기도 함
-

### RuntimeException과 언체크/런타임 예뢰

- java.lang.RuntimeException 클래스를 상속한 예외들은 명시적인 예외처리를 강제하지 않기 때문에 언체크 예외라고 불림
- 런타임 예외라고도 함
- 에러와 마찬가지로 catch문으로 잡거나 throws로 선언하지 않아도 됨 -> 명시적으로 잡거나 선언해줘도 상관없음
- 런타임 예외는 주로 프로그램 오류가 있을 때 발생하도록 의도된 것
- NullPointerException
- IllegalArgumentException 등
- -> 이런 예외는 코드에서 미리 조건을 체크하도록 주의깊게 만든다면 피할 수 있음.
- -> 피할 수 있지만, 개발자가 부주의해서 발생할 수 있는 경우에 발생하도록 만든 것이 런타임 예외
- -> 런타임 예외는 예상하지 못했던 예외 상황에서 발생하는게 아니기 때문에 굳이 catch나 throws를 사용하지 않아도 되는 것.

## 예외 처리 방법

- 먼저 예외를 처리하는 일반적인 방법 살펴보고나서 효과적인 예외처리 생각해보기

### 예외 복구

- 첫번째 예외처리 방법은 예외상황을 파악하고 문제를 해결해서 정상 상태로 돌려놓는 것.
- ex) 사용자가 파일 읽으려고 시도 중 IOException 발생
- -> 사용자에게 상황을 알려주고 다른 파일을 이용하도록 안내해서 예외상황을 해결할 수 있음
- -> 예외로 인해 기본 작업흐름이 불가능하면 다른 작업 흐름으로 자연스럽게 유도해주는 것.
- -> 이런 경우 예외 상황은 다시 정상으로 돌아오고 예외를 복구했다고 볼 수 있음.
- -> 단, IOException 에러메시지가 사용자에게 그냥 던져지는것은 예외 복구가 아님
- -> 예외가 처리 됐으면 애플리케이션에서는 정상적으로 설계된 흐름을 따라 진행되어야함.
- -> 예외처리 코드를 강제하는 **체크 예외들은 이렇게 예외를 어떤 식으로든 복구할 가능성이 있는 경우**에 사용
- -> API를 사용하는 개발자로 하여금 예외 상황이 발생할 수 있음을 인식하도록 도와주고 이에 대한 적절한 처리를 시도해보도록 요구하는 것.

```
int maxretry = MAX_RETRY;
while(maxretry-- > 0){
  try{
    //..
    return;
  }
  catch(SomeException e){
    // 로그 출력. 정해진 시간만큼 대기
  }
  finally{
    //리소스 반납, 정리 작업.
  }
}
throw new RetryFailedException(); //최대 재시도 횟수를 넘기면 직접 예외 발생
```

- 사전에 이미 성공 여부를 확인할 수 없고, 재시도가 의미 있는 경우라면 이렇게 최대 횟수만큼 반복적으로 시도함으로써 예외상황에서 복구되게 할 수 있음.

### 예외처리 회피

- 두 번째 방법은 예외처리를 자신이 담당하지 않고 자신을 호출한 쪽으로 던져버리는 것.
- throws 문으로 선언해서 예외가 발생하면 알아서 던져지게 하고나, catch문으로 일단 예외를 잡은 후에 로그를 남기고 다시 예외를 던지는 것.

```java
import java.sql.SQLException;

class ex {
    //예외처리 회피 1
    public void add() throws SQLException {
        //...
    }

    //예외처리 회피 2
    public void add() throws SQLException {
        try {
            //..
        } catch (SQLException e) {
            // 로그 출력
            throw e; //처리하고 다시 던져서 회피하는 방법
        }
    }
}
```

#### 템플릿/콜백의 예외처리 회피 예시

```java
public interface StatementStrategy { //콜백 패턴을 위한 인터페이스 -> 보통 단일 메소드를 가진 인터페이스
    PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
```

- 콜백 오브젝트는 메소드 선언을 보면 SQLException을 자신이 처리하지 않고 템플릿으로 던져버림
- -> SQLException을 처리하는 일은 콜백 오브젝트의 역할이 아니라고 보기 때문임.
- -> 콜백 오브젝트의 메소드는 SQLException에 대한 예외를 회피하고 템플릿 레벨에서 처리하도록 해준다.
- -> **예외를 회피하는 것은 예외를 복구하는 것처럼 의도가 분명해야함**.

### 예외 전환

- 예외 회피와 비슷하게 예외를 복구해서 정상적인 상태로는 만들 수 없기 때문에 예외를 메소드 밖으로 던지는 것
- -> 아지만 예외회피와 달리, 발생한 예외를 그대로 넘기는 게 아니라, 적절한 예외로 전환해서 던진다는 특징
- 두가지 목적

#### 의미를 분명하게 해줄 수 있는 예외로 바꾸기 위한 목적

- 보통 API가 발생하는 기술적인 로우레벨을 -> 비즈니스 상황에 적합한 예외로 바꿔주기 위해

```java
import java.sql.SQLException;

class ex {
    public void add(User user) throws DuplicateUserIdException, SQLException {
        try {
            //...
        } catch (SQLException e) {
            //특정 에러코드인 경우 특정 에러 던지기
            if (e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY) {
                throw DuplicaionUserIdException();
            } else {
                throw e; // 그 외는 그대로
            }
        }
    }
}
```  

- 보통은 getCause()를 이용해 전환하는 예외에 원래 발생한 예외를 담아서 중첩 예외를 만드는 것이 좋음

```
catch(SQLException e){
  throw DuplicateUserIdException(e); //생성자 이용
}
```

```
catch(SQLException e){
  throw DuplicateUesrIdException().initCause(e); //initCause() 이용
}
```

#### 예외를 처리하기 쉽고 단순하게 만들기 위해 포장하는 목적

- 중첩 예외를 이용해 새로운 예외를 만들고 원인이 되는 예외를 내부에 담아서 던지는 방식은 같음
- 주로 예외처리를 강제하는 체크예외를 언체크 예외인 런타임 예외로 바꾸는 경우에 사용
- -> 어차피 복구가 불가능한 예외라면 가능한 빨리 런타임 예외로 포장해 던지게해서 다른 계층의 메소드를 작성할 때 불필요한 예외 선언이 들어가지 않도록
- -> 런타임 예외로 포장해 던지고, 예외처리 서비스 등을 이용해 로그 남기고, 관리자에게 메일, 사용자에게 안내 메시지를 보여주는 식으로 처리하는게 바람직함.
- 반대로, 애플리케이션 로직상에서 예외조건이 발생되거나 예외상황이 발생할 수도 있음
- -> 이것은 API가 던지는 예외가 아니라 애플리케이션 코드에서 의도적으로 던지는 예외. -> 이때는 체크 예외가 적절
- -> 비즈니스적인 예외는 적절한 대응이나 복구 작업이 필요하기 때문.

## 예외처리 전략

### 런타임 예외의 보편화

- 일반적으로 **체크예외가 일반적인 예외**를 다루고,
- **언체크 예외는 시스템 장애나 프로그램상의 오류**에 사용됨
- -> 문제는 **체크 예외**는 복구가능성이 조금이라도 있는, 말그대로 예외적인 상황이기 때문에
- -> 자바는 이를 처리하는 catch 블록이나 throws 선언을 강제하고 있다는 점

#### 엔터프라이즈 서버 환경에서의 예외

- 엔터프라이스 서버 환경에선 수많은 사용자가 동시에 요청을 보내고 각 요청이 독립적인 작업으로 취급됨
- -> 하나의 요청을 처리하는 중에 예외가 발생하면 해당 작업만 중단시키면 그만
- -> 독립형 애플리케이션과 달리, 서버의 특정 계층에서 예외가 발생했을때 작업을 일시 중지하고
- -> 사용자와 바로 커뮤니케이션 하면서 예외 상황을 복구할 수 있는 방법이 없음
- -> 차라리 애플리케이션 차원에서 예외상황을 미리 파악하고 예외가 발생하지 않도록 차단하는 게 좋음.
- -> 또는 프로그램 오류나 외부 환경으로인해 예외가 밸상하는 경우라면
- -> 빨리 해당요청의 작업을 취소하고 서버 관리자나 개발자에게 통보해주는 편이 나음.
- 자바의 환경이 서버로 이동하면서 체크 예외의 활용도와 가치가 점점 떨어지고 있음.
- -> 대응이 불필요한 체크예외라면 빨리 런타임 예외로 전환해서 던지는 것이 나음.
- -> 최근엔 항상 복구할 수 있는 예외가 아니라면 일단 언체크로 만드는 경향이 있음.
- -> 언체크 예외라도 얼마든지 catch블록으로 잡아서 복구하거나 처리할 수 있음.
- -> 하지만 대개는 복구 불가능 상황이고 런타임 예외로 포장해서 던져야 하므로 아예 API 차원에서 런타임 예외를 던지도록 만들고 있는 추세

### add() 메소드의 예외처리

- SQLExeption은 대부분 복구 불가능한 예외이므로 잡아봤자 처리할 것도 없고, 결국 throws 타고 앞으로 계속 전달되다가 애플리케이션 밖으로 던져질 것
- 그럴바에는 그냥 런타임 예외로 포장해서 던져버려서 그 밖의 메소드들이 신경쓰지 않게 해주는 편이 나음.
- DuplicatedUserIdException도 굳이 체크 예외로 둬야하는 것은 아님
- DuplicatedUserIdExceptionc처럼 의미 있는 예외는 add() 메소드를 바로 호출한 오브젝트 대신 더 앞단의 오브젝트에서 다룰 수도 있음.
- -> **어디에서든 DuplicatedIdException을 잡아서 처리할 수 있다면 굳이 체크예외로 만들지 않고 런타임 예외로 만드는 게 나음**
- -> 대신 add()메소드는 명시적으로 DuplicatedUserIdException을 던진다고 선언해야함.
- 그래야 add() 메소드를 사용하는 코드를 만드는 개발자에게 의미 있는 정보를 전달해줄 수 있음.
- -> 런타임 예외도 throws로 선언할 수 있으니 문제될 것은 없음.

#### 체크 예외 대신 런타임에러로 선언 예시

```java
public class DuplicateUserIdException extends RuntimeException { //필요없다면 신경쓰지 않아도 되도록 RuntimeException을 상속..
    public DuplicateUserIdException(Throwable cause) { //중첩예외를 만들 수 있도록 생성자를 추가
        super(cause);
    }
}
```

- 사용자 아이디가 중복 됐을 때 사용하는 DuplicateUserIdException을 만듬
- 필요하다면 언제든 잡아서 처리할 수 있도록 별도의 예외로 정의하기는 하지만, 필요없다면 신경쓰지 않아도 되도록 RuntimeException을 상속한 런타임 예외로 만듬.
- 중첩예외를 만들 수 있도록 생성자를 추가.

```java
import org.user.dao.DuplicateUserIdException;

import java.sql.SQLException;

class ex {
    public void add() throws DuplicatedUserIdException {
        try {
            //SQLException을 던지는 로직
        } catch (SQLException e) {
            if (e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY) {
                throw new DuplicateUserIdException(e); // 예외 전환
            } else {
                throw new RuntimeException(e); // 예외 포장
            }
        }
    }
}
```

- -> 메소드 밖으로 던지던 SQLException을 런타임 예외로 전환해서 던지도록 함
- -> 시스템 예외에 해당하는 SQLException은 언체크로 포장
- -> 언체크로 만들었긴 했지만, 메소드 사용하는 쪽에서 아이디 중복 처리를 하고 싶은 경우 활용할 수 있음을 알려주도록
- -> DuplicatedUserIdException 메소드를 throws 선언에 포함시킴.
- -> ex) 아이디 중복 상황이라면 대신 사용 가능한 추천아이디 만들어 주는 등의 로직 추가 가능.

#### 런타임 예외의 일반화 사용

- 위처럼 런타임 예외를 일반화해서 사용하는 방법은 장점이 많음
- 컴파일러가 예외처리를 강제하지 않으므로 주의해서 사용 필요
- -> 런타임 예외를 사용하는 경우 API문서나 레퍼선스 문서를 통해, 메소드를 사용할 때 발생할 수 있는 예외의 종류와 원인, 활용방법 설명해둬야함.

### 애플리케이션 예외 (체크 예외의 활용 - 비즈니스 로직 예외)

- 런타임 예외 중심의 전략은 이름을 붙이자면 "낙관적인 예외처리"기법이라고 할 수 있음.
- -> 일단 복구할 수 있는 예외는 없다고 가정하고 예외가 생겨도 어차피 런타임 예외이므로,
- -> 시스템 레벨에서 알아서 처리해줄것이고, 꼭 필요한 경우는 런타임 예외라도 잡아서 복구하거나 대응해줄 수 있으니
- -> 문제될 것이 없다는 낙관적인 태도를 기반으로 하고 있음.
- **반면에, 시스템 또는 예외상황이 원인이 아니라, 애플리케이션 자체의 로직에 의해 의도적으로 발생시키고, 반드시 catch해서 조치를 취하도록 요구하는 예외도 있음**
- -> 이런 예외들을 일반적으로 **애플리케이션 예외.**

#### ex) 애플리케이션 예외 상황 예시

- 사용자가 은행 계좌에서 출금하는 기능을 가진 메소드
- -> 잔고를 확인해서 허용 범위를 체크하고, 중단, 경고의 경우가 포함되어야함.

##### 예외 메소드 설계 방법 두가지

- 1 정상과, 비정상 처리 경우 각각의 다른 종류의 리턴값 돌려주기
- 2 예외 상황에서는 비즈니스적 의미를 띈 예외를 던지도록하기

##### 정상과, 비정상 처리 경우 각각의 다른 종류의 리턴값 돌려주기

- 이러한 경우 메소드를 호출한 쪽에서 반드시 리턴값을 확인해서 이후 작업 흐름을 달리해야함.
- -> 예외상황에 대한 리턴값을 명확하게 코드화 하고 잘 관리하지 않으면 혼란이 발생
- -> if문 블록 범벅이 될 수 있음.

##### 예외 상황에서는 비즈니스적 의미를 띈 예외를 던지도록하기 -> 체크예외로 선언

- 잔고부족인 경우 InsufficientBalanceException 등을 던지기.
- -> 예외 상황을 처리하는 catch 블록을 메소드 호출 직후 둘 필요는 없음
- -> 정상적인 흐름을 따르지만 예외가 발생할 수 있는 코드를 try 불록안에 깔끔하게 정리해두고,
- -> 예외 상황에 대한 처리는 catch 블록에 모아 둘 수 있음
- 이때 사용하는 예외는 의도적으로 체크 예외로 만들기
- -> 개발자가 잊지 않고 잔고부족처럼 **자주 발생 가능한 예외 상황에 대한 로직을 구현하도록 강제**하는 것이 좋음.

```java
import javax.naming.InsufficientResourcesException;
import java.math.BigDecimal;

class ex {
    public void ex() {
        try {
            BigDecimal balance = account.withdraw(amount);
            //..
        } catch (InsufficientResourcesException e) { // 체크 예외 !
            // InsufficientBalanceException에 담긴 인출 가능한 잔고 금액 정보 가져옴
            BigDecimal availFunds = e.getAvailFunds();
            // 잔고 부족 안내 메시지를 준비하고 이를 출력..
        }
    }
}
```

### SQLException은 어떻게 됐나?

- SQLException의 99%상황은 복구 불가능한 상황
- ex) SQL 문법이 틀렸거나, 제약조건을 위반했거나, DB 서버가 다운됐거나, 네트워크가 불안정하거나, DB커넥션 풀이 꽉차거나..
- -> 필요없는 기계적 throws 선언 방치 말고 언체크/런테임 예외로 전환
- -> 스프링 JdbcTemplate은 이 예외처리 전략을 따르고 있음.
- -> JdbcTEmplate 템플릿과 콜백 안에서 발생하는 모든 SQLException을 런타임 예외인 DataAccessException으로 포장해서 던져줌.
- -> 꼭 **필요한 경우만 런타임 예외인 DataAccessException을 잡아서 처리**하면 되고 나머지는 무시가능.

```
public int update(final String sql) throws DataAccessException { ... }
```

- -> throws로 선언되어 있긴 하지만, DataAccessException이 런타임 예외이므로 update()를 사용하는 메소드에서 이를 잡거나 던질 의무가 없음

## 예외 전환

- 예외 전환의 목적은 두 가지
- -> 1. 굳이 필요하지 않은 catch/throws를 줄여주는 것.
- -> 2. 다른 하나는 로우레벨의 예외를 좀 더 의미있고 추상화된 예외로 바꿔서 던지는 것.

### ex) JdbcTemplate의 DataAccessException

- 런타임 예외로 SQLException을 포장해주는 역할 -> SQLExcepion에 대해 애플리케이션 레벨에서는 신경 쓰지 않도록 해줌
- 또한, SQLException으로 다루기 힘든 상세한 예외정보를 의미있고 일관성 있는 예외로 전환해서 추상화해주려는 용도도 있음.

### JDBC의 한계

- 현실적으로 db를 자유롭게 바꾸어 사용하는 DB 프로그램을 작성하는데에는 두가지 걸림돌 있음

#### 비표준 SQL

- 첫째문제는 JDBC 코드에서 사용하는 SQL.
- 대부분의 DB는 표준을 따르지 않는 비표준 문법과 기능도 제공.

#### 호환성 없는 SQLEception의 DB 에러 정보

- 문제는 DB마다 SQL만 다른 것이 아니라, 에러의 종류와 원인도 제각각이라는 점.
- -> 그래서 JDBC는 데이터 처리 중 발생하는 다양한 예외를 그냥 SQLException 하나에 담아버림.
- -> JDBC API는 이 SQLException 하나만 던지도록 설계되어 있음.
- -> 그래서 SQLException은 예외가 발생했을 떄의 DB 상태를 담은 SQL 상태정보를 부가적으로 제공
- -> getSQLState() 메소드로 예외 상황에 대한 상태정보를 가져올 수 있음
- -> DB 별로 달라지는 에러코드를 대신할 수 있도록, Open Group의 XOPEN SQL 스펙에 정의된 SQL 상태코드를 따름.
- -> 문제는 DB의 JDBC 드라이버에서 SQLException을 담을 상태코드를 정확하게 만들어주지 않음
- -> 호환성 없는 에러코드와 표준을 잘 따르지 않은 상태코드를 가진 SQLException으로 DB에 독립적인 유연한 코드 작성하는 것은 불가능

### DB 에러코드 매핑을 통한 전환

- DB 종류에 상관없이 동일한 상황에서 일관된 예외를 전달받을 수 있다면 효과적인 대응이 가능
- 스프링은 **DataAccessException**이라는 SQLException을 대체할 수 있는 런타입 예외를 정의하고 있을 뿐 아니라
- **DataAccessException의 서브클래스**로 세분화된 예외 클래스들을 정의하고 있음
- BadSqlGrammarException : SQL 문법 때문에 발생하는 에러
- DataAccessResourceFailureException : DB 커넥션을 가져오지 못했을 때 발생
- DataIntegrityViolationException : 데이터 제약조건을 위배했거나 일관성을 지키지 않은 작업을 수행
- DuplicatedKeyException : 중복키 때문에 발생한 경우
- -> 스프링은 DB별로 에러코드를 분류해서 스프링이 정의한 예외 클래스와 매필해놓은 에러 코드 매핑 정보 테이블을 만들어주고 이를 이용

#### DataAccessException

- JdbcTemplate은 SQLException을 단지 런타임 예외인 DataAccessException으로 포장하는 것이 아니라
- -> DB의 에러 코드를 DataAccessException 계층구조의 클래스 중 하나로 매핑시켜줌.
- -> DB가 달라져도 같은 종류의 에러라면 동일한 예외를 받을 수 있음

```java
class ex {
    public void add() throws DuplicateKeyException {
        //JdbcTEmplate을 이용해 User를 add하는 코드
    }
}
```

- -> JdbcTemplate은 체크 예외인 SQLException을 런타임 예외인 DataAccessException 계층구조의 예외로 포장해주기 때문에
- -> add()에 예외 포장을 의한 코드가 필요 없음
- -> add() 메소드를 사용하는 쪽에서 중복키 상황에 대한 대응이 필요한 경우 참고할 수 있도록
- -> **DuplicateKeyException을 선택적(런타임예외인DataAccessException의 서브 클래스)**으로 메소드 선언에 넣어주면 편리

### DAO 인터페이스와 DataAccessException 계층 구조

- DataAccessException은 의미가 같은 예외라면 데이터 엑세스 기술의 종류와 상관없이 일관된 예외가 발생하도록 만들어줌.

#### DAO 인터페이스와 구현의 분리

- DAO를 따로 만들어서 사용하는 이유 두가지
- 1 데이터 엑세스 로직을 담은 코드를 성격이 다른 코드에서 분리해놓기 위해
- 2 분리된 DAO는 전략패턴을 적용해 구현 방법을 변경해서 사용할 수 있게 만들기 위해서
- -> DAO의 사용 기술과 구현 코드는 전략패턴과 DI를 통해서 DAO를 사용하는 클라이언트에게 감출 수 있지만
- -> 메소드 선언에 나타나는 예외정보가 문제가 될 수 있음

```java
public interface UserDao {
    public void add(User user); // 이렇게 선언하는 것이 가능?
}
```

- UserDao의 인터페이스르 분리해서 기술에 독립적인 인터페이스로 만들려면 위처럼 정의해야함
- 하지만 DAO에서 사용하는 데이터 엑세스 기술의 API가 예외를 던지기 떄문에 위 같은 메소드 선언은 사용할 수 없음.
- 만약 JDBC API를 사용하는 UserDao 구현 클래스의 add()메소드라면 SQLExceptino을 던질 것
- -> 인터페이스의 메소드 선언에 없는 예외를 구현 클래스 메소드의 throws에 넣을 수 없음

```java
public interface UserDao {
    public void add(User user) throws SQLException;
}
```

- 그러나 위처럼 정의한 메소드는 JDBC가 아닌 기술로 DAO를 구현하면 사용할 수가 없음. 기술들이 자신만의 독자적인 예외를 던지기 떄문

```
public void add(User user) throws PersistentException; //JPA
public void add(User user) throws HibernateException; //Hibernate
public void add(User user) throws JdoException; //JDO
```

- -> 구현 기술마다 던지는 예외가 다르므로 기술에 완전히 독립적으로 만들 수 없는 상황
- -> 단지 인터페이스로 추상화하고 일부 기술에서 발생하는 체크예외를 런타임 예외로 전환하는 것만으로는 불충분.

#### 데이터 엑세스 예외 추상화와 DataAccessException 계층구조

- 그래서 스프링은 자바의 다양한 데이터 엑세스 기술을 사용할 때 발생하는 예외들을 추상화해서 DataAccessException 계층구조안에 정리해놓음
- 앞서 JdbcTemplate에서 보았떤 DataAccessException 클래스들이 단지 SQLException을 전환하는 용도로만 쓰이는 건 아님.
- **DataAccessException**은 자바의 주요 **데이터 엑세스 기술에서 발생할 수 있는 대부분 예외를 추상화**하고 있음.
- InvalidDataAccessResourceUsageException : 엑세스 기술을 부정확하게 사용
- OjbectOptimisticLockingFailureException : ORM에서 낙관적인 락킹 발생 시
- ex) 직접 낙관적인 락킹 기능을 구현했다고 하면 슈퍼클래스인 OptimisticLockingFailureException을 상속해서 정의할 수도 있음
  ![](/img/img_9.png)

- IncorrectResultSizeDataAccessException : 쿼리실행결과의 사이즈 다를떄 -> queryForObject는 서브클래스인 EmptyResultDataAccessException을 발생

### 기술에 독립적인 UserDao 만들기

#### 인터페이스 적용

- UserDao 클래스를 인터페이스와 구현으로 분리해보기

```java
public interface UserDao {
    void add(User user);

    User get(String id);

    List<User> getAll();

    void deleteAll();

    int getCount();
}
```

- 의존 관계 수정

```java

@Configuration
public class DaoFactory {
    //..
    @Bean
    public UserDaoJdbc userDao() { //빈의 이름은 클래스 이름이 아니라 구현 클래스의 이름을 따름
        UserDaoJdbc userDaoJdbc = new UserDaoJdbc();
        userDaoJdbc.setDataSource(dataSource());
        return userDaoJdbc;
    }
}
```

#### DataAccessException 활용 시 주의사항

- DuplicateKeyException 같은 경우 아직까지는 JDBC를 이용하는 경우에만 발생함.
- -> 따라서, DAO에서 사용하는 기술의 종류와 상관없이 동일한 예외를 얻고 싶다면 DuplicatedUserIdException 처럼 직접 예외를 정의해두고,
- -> 각 DAO의 add() 메소드에서 좀 더 상세한 예외 전환을 해줄 필요가 있다.
- -> SQLException을 직접 해석해서 DataAccessException으로 변환해주는 코드의 사용법 살펴보기...

##### SQLExceptionTranslator

- DB에러 코드를 이용하여, SQLExceptionTranslator 인터페이스를 구현한 클래스 중에서
- -> SQLErrorCodeSQLExceptionTranslator를 사용하기.
- -> 에러코드 변환에 필요한 DB의 종류를 알아내기 위해 현재 연결된 DataSource를 필요로함.

```java
class ex {
    @Test
    public void sqlExceptionTranslate() {
        dao.deleteAll();
        try {
            dao.add(user1);
            dao.add(user1);
        } catch (DuplicateKeyException ex) {
            SQLException sqlEx = (SQLException) ex.getRootCause();
            SQLExceptionTranslator set =
                    new SQLErrorCodeSQLExceptionTranslator(this.dataSource);

            assertThat(set.translate(null, null, sqlEx), is(DuplicateKeyException.class))
        }
    }
}
```

## 정리

- 예외를 잡아서 아무런 조취를 취하지 않거나 의미없는 thorws 선언을 남발하는 것은 위험
- 예외는 복구하거나 예외처리 오브젝트로 의도적으로 전달하거나 적절한 예외로 전환해야함
- 예외 전환 : 좀 더 의미있는 예외로 변경 or 불필요한 catch/throws를 피하기 위해 런타임 예외로 포장
- **복구할 수 없는 예외**는 가능한 **빨리 런타임 예외로** 전환
- **애플리케이션의 로직**을 담기 위한 예외는 **체크 예외로**
- JDBC의 SQLException은 대부분 복구할 수 없는 예외이므로 런타임 예외로 포장해야함
- SQLException의 에러코드는 DB에 종속되기 때문에 DB에 독립적인 예외로 전환될 필요가 있음
- 스프링은 DataAccessException을 통해 DB에 독립적으로 적용 가능한 추상화된 런타임예외 계층을 제공함
- DAO를 데이터 엑세스 기술에서 독립시키려면 인터페이스 도입과 런타임 예외 전환, 기술에 독립적인 추상화된 예외로 전환이 필요함.

# 5장 서비스 추상화

- DAO에 트랜잭션을 적용해보면서 스프링이 어떻게 성격이 비슷한 여러 종류의 기술을 추상화하고 일관된 방법으로 사용할 수 있도록 지원하는지 살펴보기

## 사용자 레벨 관리 기능 추가

```
- 구현해야할 비즈니스 로직
- 사용자의 레벨은 BASIC, SIVER, GOLD 세가지 중 하나
- 사용자가 처음 가입하면 BASIC 레벨이 되며, 이후 활동에 따라서 한 단계씩 업그레이드될 수 있음
- 가입 후 50회 이상 로그인 하면 BASIC에서 SILVER 레벨이 됨
- SILVER 레벨이면서 30번 이상 추천을 받으면 GOLD 레벨이 됨
- 사용자 레벨의 변경 작업은 일정한 주기를 가지고 일괄적으로 진행됨. 변경작업 전에는 조건을 충족하더라도 레벨의 변경이 일어나지 않음.
```

### 필드 추가

#### LEVEL 이늄

- 먼저 User 클래스에 사용자의 레벨을 저장할 필드를 추가
- User에 추가할 프로퍼티 타입이 숫자면 타입이 안전하지 않아서 위험할 수 있음.

```java
// 예시 안전하지 않은 타입 예시
public class User {
    private static final int BASIC = 1;
    private static final int SILVER = 2;
    private static final int GOLD = 3;

    int level;
    //..

    public void setLevel(int level) {
        this.level = level;
    }
}
```

```
// 사용자 레벨 상수 값을 이용한 코드
if(user1.getLevel() == User.BASIC){
  user1.getLevel(User.SILVER);
}
```

- -> 문제는 level의 타입이 int 이기 떄문에 다음처럼 다른 종류의 정보를 넣는 실수를 해도 컴파일러가 체크해주지 못함!

```
user1.setLevel(1000);
```

- -> 따라서 숫자타입을 직접 사용하는 것보다 이늄을 이용하는게 안전하고 편리함!

```java
public enum Level {
    BASIC(1), SILVER(2), GOLD(3);
    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int intValue() {
        return value;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1:
                return BASIC;
            case 2:
                return SILVER;
            case 3:
                return GOLD;
            default:
                throw new AssertionError("Unknown value: " + value);
        }
    }
}
```

```java
// User필드에 Enum 추가
public class User {
    Level level;
    int login;
    int recommend;

    //...
    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}
```

#### 사용자 수정 기능 추가

- 수정할 정보가 담긴 User오브젝트를 전달하면 id를 참고해서 사용자를 찾아
- 필드 정보를 UPDATE문을 이용해 모두 변경해주는 메소드 만들기

#### 수정 기능 테스트 추가

```java
class ex {
    @Test
    public void update() {
        dao.deleteAll();
        dao.add(user1);
        user1.setName("오민규");
        user1.setPassword("springno6");
        user1.setLevel(Level.GOLD);
        user1.setLogin(1000);
        user1.getRecommend(999);
        dao.update(user1);

        User user1update = dao.get(user1.getId());
        checkSameUser(user1, user1update);
    }
}
```

```java
class ex {
    public void update(User user) {
        this.jdbcTemplate.update(
                "update users set name = ?, password = ?, level = ?, login = ? " +
                        "recommend = ? where id =? ", user.getName(), user.getPassword(),
                user.getLevel().intValue(), user.getLogin(), user.getRecommend(),
                user.getId());
    }
}
```

#### 수정 테스트 보완

- update문이 실행되어도 정말 값이 수정되었는지 확인하는 로직 추가하기.

```java
class ex {
    @Test
    public void update() {
        dao.deleteAll();

        dao.add(user1);
        dao.add(user2);
        dao.add(user3);

        user1.setName("오민규");
        user1.setPassword("springno6");
        user1.setLevel(Level.GOLD);
        user1.setLogin(1000);
        user1.setRecommend(999);

        dao.update(user1);

        User user1update = dao.get(user1.getId());
        checkSameUser(user1, user1update);
        User user2same = dao.get(user2.getId());
        checkSameUser(user2, user2same);
    }
}
```

## UserService.updateLevels()

- 사용자 관리 로직은 어디에 두는 것이 좋을까?
- DAO는 데이터를 어떻게 가져오고 조작할지 다루는 곳이지 비즈니스 로직을 두는 곳이 아님
- 사용자 관리 비즈니스 로직을 담을 클래스를 추가 -> UserService

### UserService 클래스와 빈 등록

```java
public class UserService {
    UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

#### updateLevels() 메소드

```java
class ex {
    public void upgradeLevels() { //사용자 레벨 관리 기능
        List<User> users = userDao.getAll();
        for (User user : users) {
            Boolean changed = null;
            if (user.getLevel() == Level.BASIC && user.getLogin() >= 50) {
                user.setLevel(Level.SILVER);
                changed = true;
            } else if (user.getLevel() == Level.SILVER && user.getRecommend() >= 30) {
                user.setLevel(Level.GOLD);
                changed = true;
            } else if (user.getLevel() == Level.GOLD) {
                changed = false;
            } else {
                changed = false;
            }
            if (changed) {
                userDao.update(user);
            }
        }
    }
}
```

#### upgradeLevels() 테스트

- 테스트 픽스처의 개수가 UserDaoTest에서보다 많아졌으니 각각 변수로 등록하는 리스트를 사용

```java
class ex {
    List<User> users;

    @Before
    public void setUp() {
        //..
        users = Arrays.asList(
                new User("bumin", "박범진", "p1", Level.BASIC, 49, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, 50, 0),
                new User("erwins", "신승한", "p3", Level.SILVER, 60, 29),
                new User("mdnite1", "이상호", "p4", Level.SILVER, 60, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }

    @Test
    public void updateLevels() {
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }
        userService.upgradeLevels();

        checkLevel(users.get(0), Level.BASIC);
        checkLevel(users.get(1), Level.SILVER);
        checkLevel(users.get(2), Level.SILVER);
        checkLevel(users.get(3), Level.GOLD);
        checkLevel(users.get(4), Level.GOLD);
    }

    private void checkLevel(User user, Level expectedLevel) {
        User userUpdate = userDao.get(user.getId());
        assertThat(userUpdate.getLevel(), is(expectedLevel));
    }
}
```

#### UserService.add()

- 처음 가입하는 사용자는 BASIC레벨로 설정하는 로직
- 사용자 관리에 대한 비즈니스 로직을 담고 있는 UserService에 이 로직을 넣기

##### 테스트 작성

- User오브젝트에 level 미리 설정되어 있는 경우는 어떻게?
- -> level이 비어있어면 BASIC, 미리 설정되어 있으면 그대로.

```java
class ex {
    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4); //레벨 이미 지정된 User라면 초기화x
        User userWithoutLevel = users.get(0); //레벨 비어있으면 초기화
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel(), is(userWithLevelRead.getLevel()));
        assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
    }
}
```

```java
class ex {
    public void add(User user) {
        if (user.getLevel() == null) {
            user.setLevel(Level.BASIC);
        }
        userDao.add(user);
    }
}
```

### 코드 개선

#### upgradeLevels()의 문제점

- for 루프속의 if/else 블록들이 읽기 불편함
- 레벨 변화단계와 업그레이드 조건, 조건 충족시 처리 작업이 섰여있어서 로직 이해하기가 어려움
- -> 성격이 다른 여러가지 로직이 한데 섞여 있기 때문임

```java
class ex {
    public void upgradeLevels() { //사용자 레벨 관리 기능
        List<User> users = userDao.getAll();
        for (User user : users) {
            Boolean changed = null;
            // 현재 레벨이 무엇인지 판단하는 로직 && 업그레이드 조건을 담은 로직
            if (user.getLevel() == Level.BASIC && user.getLogin() >= 50) {
                user.setLevel(Level.SILVER); // 다음 단계 레벨이 무엇이고 업그레이드를 위한 작업은 어떤 것인지
                changed = true; //5의 작업을 위한 플래그
            } else if (user.getLevel() == Level.SILVER && user.getRecommend() >= 30) {
                user.setLevel(Level.GOLD);
                changed = true;
            } else if (user.getLevel() == Level.GOLD) {
                changed = false;
            } else {
                changed = false; // BASIC이 로그인횟수 50 되지 않을 경우도 else, 새로운 레벨이 추가되도 else -> 성격이 다른 경우가 모두 한곳에서 처리되고 있음.
            }
            if (changed) {
                userDao.update(user);
            }
        }
    }
}
```

#### updateLevels() 리팩토링

- 구체적인 구현에서 외부에 노출할 인처페이스를 분리하는 것처럼
- -> 레벨을 업그레이드하는 작업의 기본 흐름만 먼저 만들어보기

```java
class ex {
    public void upgradeLevels() {
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }
}
```

- 모든 사용자 정보를 가져와 한 명씩 업그레이드가 가능한지 확인하고 / 가능하면 업그레이드를 한다.

```java
class ex {
    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC:
                return (user.getLogin() >= 50);
            case SILVER:
                return (user.getRecommend() >= 30);
            case GOLD:
                return false;
            default:
                throw new IllegalArgumentException("Unkown Level: " + currentLevel);
        }
    }

    private void upgradeLevel(User user) { // 덜 클린한 상황
        if (user.getLevel() == Level.BASIC) {
            user.setLevel(Level.SILVER);
        } else if (user.getLevel() == Level.SILVER) {
            user.setLevel(Level.GOLD);
        }
        userDao.update(user);
    }
}
```

- updateLevel() 리팩토링할 부분
- -> 다음단계가 무엇인가 하는 로직과 그때 사용자 오브젝트의 level 필드를 변경해준다는 로직이 함께 있는데다, 너무 노골적으로 드러나있음.
- -> 예와상황도 없어서 다음단계까 없는 GOLD레벨인 사용자를 업그레이드하려고 이 메소드를 호출한다면 아무것도 처리하지 않고 그냥 DAO의 업데이트 메소드만 실행될 것.
- -> 레벨이 늘어나면 if문도 점점 길어질것..

#### upgradeLevel() 리팩토링

- 먼저 레벨의 순서와 다음 단계의 레벨이 무엇인지 결정하는 일은 Level에게 맡기기(레벨의 순서를 UserSevice에 담아둘 이유가 없음)

```java
public enum Level {
    GOLD(3, null), SILVER(2, GOLD), BASIC(1, SILVER);

    //..
    public Level nextLevel() {
        return this.next;
    }
    //..
}
```

- 사용자 정보가 바뀌는 부분을 UserService메소드에서 User로 옮기기.
- -> User의 내부정보가 변경되는 것은 UserService보다는 User가 다루는 게 적절함.

```java
class User {
    //..
    public void upgradeLevel() {
        Level nextLevel = this.level.nextLevel();
        if (nextLevel == null) {
            throw new IllegalStateException((this.level) + "은 업그레이드가 불가능합니다.");
        } else {
            this.level = nextLevel;
        }
    }
}
```

- 추후, 업그레이드 시 기타 정보도 변경이 필요해지면 필드를 추가하고 upgradeLevel()에서 처리하면 됨.

```java
class UserService {
    //..
    private void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
    }
}
```

#### UserServiceTest

```java
class UserServiceTest {
    @Test
    public void upgradeLevel() {
        Level[] levels = Level.values();
        for (Level level : levels) {
            if (level.nextLevel() == null) continue;
            user.setLevel(level);
            user.upgradeLevel();
            assertThat(user.getLevel(), is(level.nextLevel()));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void cannotUpgradeLevel() {
        Level[] levels = Level.values();
        for (Level level : levels) {
            if (level.nextLevel() != null) continue; //null 찾기
            user.setLevel(level);
            user.upgradeLevel();
        }
    }
}
```

#### UserServiceTest 개선

```java
class ex {
    @Test
    public void updateLevels() {
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }
        userService.upgradeLevels();

//        checkLevel(users.get(0), Level.BASIC);
//        checkLevel(users.get(1), Level.SILVER);
//        checkLevel(users.get(2), Level.SILVER);
//        checkLevel(users.get(3), Level.GOLD);
//        checkLevel(users.get(4), Level.GOLD);
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);
    }

    private void checkLevel(User user, Level expectedLevel) {
        User userUpdate = userDao.get(user.getId());
        assertThat(userUpdate.getLevel(), is(expectedLevel));
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());
        if (upgraded) {
            assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
        } else {
            assertThat(userUpdate.getLevel(), is(user.getLevel()));
        }
    }
    //..
}
```

- 기존 upgradeLevels() 테스트 코드는 테스트 로직이 분명하게 드러나지 않은 것이 단점
- checkLevel()을 호출하면서 파라미터로 Level이늄을 하나 전달하는데, 테스트 코드만 봐서는 그것이 업그레이드된 경우를 테스트하려는 것인지 쉽게 파악이 안됨.
- -> 개선된 코드에서는 각 사용자에 해대 업그레이드를 확인하려는 것인지 boolean으로 나타나 있음.
- -> 또 업그레이드 됐을 떄 어떤 레벨인지는 Level이늄의 nextLevel()을 호출해보면 됨

#### 애플리케이션 코드와 테스트코드 사이의 중복 제거

```
case BASIC: return(user.getLogin() >= 50);  //UserService 

new User("joytouch", "강명성", "p2" Level.BASIC, 50, 0) // UserServiceTest -> 50 설정해주는것이 중복.. -> 상수값 처리
```

- 정수형 상수 선언하기

```java
public class UserService {
    public static final int MIN_LOGCOUNT_FOR_SILVER = 50;
    public static final int MIN_RECCOMEND_FOR_GOLD = 30;

    //..
    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC:
                return (user.getLogin() >= MIN_LOGCOUNT_FOR_SILVER);
            case SILVER:
                return (user.getRecommend() >= MIN_RECCOMEND_FOR_GOLD);
            //..
        }
    }
}
```

```java
import static org.user.service.UserService.MIN_LOGCOUNT_FOR_SILVER;
import static org.user.service.UserService.MIN_RECCOMEND_FOR_GOLD;

//..
public class UserDaoJdbcTest {
    @Before
    public void setUp() {
        //..
        users = Arrays.asList(
                new User("bumin", "박범진", "p1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER - 1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
                new User("erwins", "신승한", "p3", Level.SILVER, MIN_RECCOMEND_FOR_GOLD - 1, 29),
                new User("mdnite1", "이상호", "p4", Level.SILVER, MIN_RECCOMEND_FOR_GOLD, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }
    //..
}
```

- -> 숫자로만 되어 있는 경우 비즈니스로직을 이해하기 어려웠던 부분이 의도 파악이 가능하게 됨.

#### 유저등급 정책 변경 분리하기

- 레벨을 업그레이드하는 정책을 유연하게 변경할 수 있도록 개선
- 중요로직인 UserService의 수정을 최소화 하기 위해 정책 분리 -> DI를 통해 정책 주입 -> 정책 인터페이스 만들기

```java
public interface UserLevelUpgradePolicy {
    boolean canUpgradeLevel(User user);

    void upgradeLevel(User user);
}
```

## 트랜잭션 서비스 추상화

- 정기 사용자 레벨 관리 작업 도중 실패하면?

### 모 아니면 도

- -> 이미 변경된 사용자 레벨은 작업 이전상태 ?
- -> 아니면 바뀐 채로 남아 있을까?

#### 장애 테스트

- 장애 상황을 일부러 만들 수는 없음
- -> 장애 발생했을 때 일어나는 현상 중의 하나인 예외가 던져지는 상황을 의도적으로 만들기

#### 테스트용 UserService 대역

- 예외를 강제 발생시키도록 애플리케이션 코드 수정할 수 없으므로
- -> 테스트용으로 만든 UserService의 대역을 사용하는 방법이 좋음.
- -> 간단히 UserService를 상속해서 테스트에 필요한 기능을 추가하도록 일부 메소드를 오버라이딩 하는 방법
- -> 테스트에서만 사용할 클래스라면 테스트 클래스 내부에 스태틱 클래스로 만드는 것이 간편.
- -> **테스트를 위해 소스를 수정하는것은 지양**해야겠지만
- -> UserService의 메소드가 private이여서 **최소한으로 protected로 수정**하기

```
protected void upgradeLevel(User user) {...}
```

```java
class UserServiceTest {
    //..
    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            // 지정된 id의 User 오브젝트가 발견되면 예외를 던져서 작업을 강제로 중단.
            if (user.getId().equals(this.id)) throw TestUserServiceException();
            super.upgradeLevel(user);
        }
    }

    // 테스트 목적을 띤 예외를 정의
    static class TestUserServiceException extends RuntimeException {
    }
}
```

- 다른 예외가 발생했을 경우와 구분하기 위해 테스트 목적을 띤 예외를정의해두기
- -> 테스트 서비스와 같이 테스트 클래스내 스태틱 멤버로 만들기

#### 강제 예외 발생을 통한 테스트

- 사용자 레벨 업그레이드를 시도하다가 중간에 예외가 발생했을 경우, 그 전에 업그레이드했떤 사용자도 원래 상태로 돌아갔는지 확인..

```java
class UserServiceTest {
    //..
    @Test
    public void upgradeAllOrNothing() {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao); // 수동 DI
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }
        try {
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected"); // 예외 테스트이므로 정상종료라면 실패
        } catch (TestUserServiceException e) {
            //TestUserService가 던져주는 예외를 잡아서 계속 진행되도록 함.
        }
        checkLevelUpgraded(users.get(1), false);  // users.get(1)의 인스턴스는 레벨 업데이트 된 상태
    }
}
```

- -> 테스트는 실패함 두번째 사용자 레벨이 BASIC에서 SIVLER로 바뀐것이 네번째 사용자 처리 중에 예외 발생했지만 그대로 유지되는 중

#### 테스트 실패의 원인

- updateLevels()가 하나의 트랜잭션 안에서 동작하지 않았기 때문

### 트랜잭션 경계설정

- 두가지 작업이 하나의 트랜잭션이 되려면, 두번째 SQL이 성공적으로 DB에서 수행되기 전에 문제가 발생한 경우, 앞에서 처리한 SQL 작업도 취소시켜야함.
- -> 이런 취소 작업을 트랜잭션 롤백이라고 함.
- -> 반대로 여러개의 SQL을 하나의 트랜잭션으로 처리하는 경우 모든 SQL 수행작업이 다 성공적으로 마무리 됐다고 DB에 알려서 작업을 확장 -> 트랜잭션 커밋

#### JDBC 트랜잭션의 트랜잭션 경계 설정

- 트랜잭션은 시작하는 방법은 한가지지만, **끝나는 방법은 롤백과, 커밋 두가지**.
- 애플리케이션 내에서 트랜잭션이 시작되고 끝나는 위치를 트랜잭션의 경계라고 부름
- 트랜잭션 적용 간단 예제)

```java
class ex {
    public void ex() {
        Connection c = dataSource.getConntection(); //DB 커넥션
        c.setAutoCommit(false); //트랜잭션 시작...
        try {
            PreparedStatment st1 = c.prepareStatement("...");
            st1.executeUpdate();

            PreparedStatment st2 = c.prepareStatement("...");
            st2.executeUpdate();

            c.commit(); //트랜잭션 커밋
        } catch (Exception e) {
            c.rollback(); // 트랜잭션 롤백  
        } //트랜잭션 끝...
        c.close();
    }
}  
```

##### Connection

- Jdbc 트랜잭션은 하나의 Connection을 가져와 사용하다 닫는 사이에서 일어남.
- 트랜잭션 시작과 종료는 Connection 오브젝트를 통해 이뤄짐.
- Jdbc에서 트랜잭션을 시작하려면 자동커밋 옵션을 false로 만들어주면 됨. -> jdbc의 기본 설정은 db작업 수행 직후 자동 커밋
- -> 작업 마다 커밋해서 트랜잭션을 끝내버리므로 여러개의 db 작업을 모아서 트랜잭션을 만드는 기능이 꺼져 있음..
- -> 이렇게 **하나의 DB 커넥션 안에서 만들어지는 트랜잭션을 로컬 트랜잭션**이라고도 함

#### UserService와 UserDao의 트랜잭션 문제

- 위의 UserService의 upgradeLevels()에 트랜잭션이 적용되지 않은 이유
- -> 트랜잭션을 시작하고, 커밋하고, 롤백하는 트랜잭션 경계설정코드가 존재하지 않았음.
- -> **JdbcTemplate 사용한 후 Connection 오브젝트가 숨겨짐**..

##### JdbcTemplate에서의 Connection

- JdbcTemplate은 직접 만들어봤던 JdbcContext와 작업 흐름이 거의 동일함.
- 하나의 템플릿 메소드 안에서 DataSource의 getConnection() 메소드를 호출해서 Conntection오브젝트를 가져오고,
- -> 작업을 마치면 Connection을 확실하게 닫아주고 템플릿 메소드를 빠져나온다..
- -> 결국 템플릿 메소드 호출 반 번에 한 개의 DB 커넥션이 만들어지고 닫히는 일까지 일어나는것.
- -> 일반적으로 트랜잭션은 커넥션 보다도 존재 범위가 짧다. -> 메소두 호출될때마다 트랜잭션 시작, 메소드 끝나기전 트랜잭션 종료
- -> 결국, JdbcTemplate의 메소드를 사용하는 UserDao는 각 메소드마다 하나씩 독립적인 트랜잭션으로 실행될 수밖에 없음.
- --> upgradeLevels()와 같이 여러 번의 DB에 업데이트 해야하는 작업을 하나의 트랜잭션으로 만들려면 어떻게 해야할까?

#### 비즈니스 로직 내의 트랜잭션 경계설정

- UserService와 UserDao를 그대로 둔 채로 트랜잭션을 적용하려면 결국 트랜잭션의 결계설정 작업을 UserSerivce쪽으로 가져와야함.
- 트랜잭션 경계를 updateLevels() 메소드 안에 두려면 DB 커넥션도 이 메소드 안에서 만들고, 종료시킬 필요가 있음.

##### UserService에서 Connection 만들기..?

- -> UserService에서 Connection 만들어서 UserDao에 전달... -> 메소드 호출마다 Connection 오브젝트 파라미터로 전달..

```java
public interface UserDao {
    public void add(Connection c, User user);

    public User get(Connection c, String id);

    //...
    public void update(Connection c, User user1);
}
```

- UserService에서 생성된 Connection이 레파지토리 레이어까지 이어서 물고 전달되어야하는 상황...

```java
class UserService {
    public void upgradeLevels() throws Exception {
        Connection c = ...;
        //..
        try {
            upgradeLevel(c, user);
        } catch (Exception e) {
            //..
        }
        //..
    }

    public void upgradeLevel(Connection c, User user) {
        user.upgradeLevel();
        userDao.update(c, user);
    }
}

interface UserDao {
    public update(Connection c, User user);
}
```

#### UserService 트랜잭션 경계설정의 문제점

- JdbcTemplate을 사용x. jdbc api를 직접 사용하는 초기 방식으로 돌아옴
- DAO의 메소드들과 UserService들의 메소드들에 Connection 파라미터가 추가되어야한다는 점
- Connection 파라미터가 UserDao인터페이스 메소드에 추가되면 더이상 DB 엑세스 기술에 독립적일수가 없음.

### 트랜잭션 동기화

#### Connection 파라미터 제거

- 스프링이 제안하는 방법은 **독립적인 트랜잭션 동기화** 방식.
- 트랜잭션 동기화란 UserService에서 트랜잭션을 시작하기 위해 만든 **Connection 오브젝트를 특별한 저장소에 보관**해두고,
- -> 이후에 호출되는 **DAO의 메소드에서 저장된 Connection을 가져다가 사용**하게 하는 것.
  ![](img/img_9.png)
- (1) UserService는 Connection을 생성
- (2) 이를 트랜잭션 동기화 저장소에 저장해두고 Connection의setAutoCommit(false)호출하여 트랜잭션 시작
- (3) 첫 번쨰 update() 메소드가 호출되고, JdbcTemplate 메소드에서
- (4) 트랜잭션 저장소에 현재 시작된 트랜잭션을 가진 Connection 오브젝트가 존재하는지 확인.
- (5) Connection을 이용해 PreparedStatement를 만들어 수정 SQL을 실행
- 트랜잭션 동기화 저장소에서 DB 커텍션을 가져왔을 경우 JdbcTemplate은 Connection을 닫지 않은 채로 작업 마침
- (6) 두 번째 update()가 호출되면 이떄도 마찬가지로 (7) 트랜잭션 동기화 저장소에서 Connection을 가져와 (8)을 사용함
- (9) 마지막 update()도 (10) 같은 트랜잭션을 가진 Connection을 가져와 (11) 사용
- 트랜잭션 내의 모든 작업이 정상적으로 끝났으면 UserService는 이제 (12) Connection의 commit()을 호출해서 트랜잭션을 완료시킴
- (13) 마지막으로 트랜잭션 저장소가 더이상 Connection오브젝트를 저장하지 않도록 이를 제거
- -> 어느 작업이라도 예외상황이 발생하면 UserService는 Connection의 rollback()을 호출하고 트랜잭션을 종료.
- -> 이때도 트랜잭션 저장소에 저장된 동기화된 Connection 오브젝트는 제거해줘야함.
- 트랜잭션 동기화 저장소는 작업 스레드마다 독립적으로 Connection 오브젝트를 저장하고 관리하기 때문에
- -> 다중 사용자를 처리하는 서버의 멀테스레드 환경에서도 충돌이 날 염려 없음.

#### 트랜잭션 동기화 적용

- 스프링은 JdbcTemplate과 더불어 이런 트랜잭션 동기화 기능을 지원하는 간단한 유틸리티 메소드를 제공함.

```java
public class UserService {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void upgradeLevels() throws Exception {
        TransactionSynchronizationManager.initSynchronization(); // 트랜잭션 동기화 관리자를 이용해 동기화 작업을 초기화
        Connection c = DataSourceUtils.getConnection(dataSource);
        c.setAutoCommit(false);

        try {
            List<User> users = userDao.getAll();
            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            c.commit();
        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            DataSourceUtils.releaseConnection(c, dataSource); //db 커넥션 닫기 
            TransactionSynchronizationManager.unbindResource(this.dataSource);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
```

- DataSourceUtils에서 제공하는 getConnection() 메소드를 통해 DB 커넥션을 생성함.
- DataSourceUtils의 getConnection() 메소드는 Connection 오브젝트 생성해줄뿐만아니라, 트랜잭션 동기화에 사용하도록 저장소에 바인딩해줌

#### JdbcTemplate 트랜잭션 동기화

- JdbcTemplate은 update()나 query()같은 JDBC 작업의 템플릿 메소드를 호출하면 직접 Connection을 생성하고 종료하는 일을 담당
- 트랜잭션 동기화 저장소에 등록된 DB 커넥션이나 트랜잭션이 없는 경우에는 JdbcTemplate이 직접 DB 커넥션을 만들고 트랜잭션을 시작해서 JDBC 작업을 진행함.
- 트랜잭션 동기화 저장소에 커넥션이 있다면 가져와서 사용함.

### 트랜잭션 서비스 추상화

#### 기술과 환경에 종속되는 트랜잭션 경계 설정 코드

- 여러개의 DB를 사용하고 있을 경우의 이슈 -> 하나의 트랜잭션 안에서 여러개의 DB 데이터 넣는 작업이 필요
- 로컬 트랜잭션은 하나의 DB Connection에 종속됨
- -> 별도의 **트랜잭션 관리자를 통해 트랜잭션 관리하는 글로벌 트랜잭션** 방식 사용해야함
- -> JTA : Java Transaction API
  ![](img/img_10.png)
- 애플리케이션에서는 기존방법대로 DB는 JDBC, 메시징서버라면 JMS 같은 API를 사용해서 필요한 작업을 수행
- 트랜잭션은 JDBC나 JMS API를 사용해서 직접 제어하지 않고, JTA를 통해 트랜잭션 매니저가관리하도록 위임함
- 트랜잭션 매니저는 DB와 메시징 서버를 제어하고 관리하는 각각의 리소스 매니저와 XA프로토콜을 통해 연결됨
- -> 분산 트랜잭션 또는 글로벌 트랜잭션 가능

```java
class ex {
    void exMethod() {
        InitialContext ctx = new InitialContext();
        UserTransaction tx = (UserTransaction) ctx.lookup(USER_TX_JNDI_NAME);
        tx.begin();
        Connection c = dataSource.getConnection(); // JNDI로 가져온 dataSource 사용해야함. 
        try {
            // 데이터 엑세스 코드
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            c.close();
        }
    }
}
```

- JTA를 이용한 방법으로 바뀌긴 했지만
- -> Connection 메소드 대신에 UserTransation메소드를 사용한다는 점을 제외하면 트랜잭션 처리 방법은 별로 달라진 게 없음
- -> 문제는 UserService 코드를 수정해야한다는 점
- -> 또 문제는 하이네이트 등을 이용한 트랜잭션 관리 코드는 JDBC나 JTA의 코드와는 또 다르다는 것.
- 하이버네이트는 Connection을 직접사용하지 않고 Session이라는 것을 사용. 독자적인 트랜잭션관리 API를 사용

#### 트랜잭션 API의 의존관계 문제와 해결책

![](img/img_11.png)

- 원래 UserService는 UserDao 인터페이스에만 의존하는 구조였음.
- -> JDBC에 종속적인 Connection 을 이용한 트랜잭션 코드가 UserService에 등장하면서부터
- -> UserService는 UserDaoJdbc에 간접적으로 의존하는 코드가 되버림..
- -> 다행히 트랜잭션 경계설정을 담담하는 코드는 일정한 패턴을 갖는 유사한 구조
- -> 이렇게 여러 기술의 사용방법에 공통점이 있다면 추상화를 생각해볼 수 있음.
- -> **추상화**란 하위 시스템의 공통점을 뽑아내서 분리시키는 것
- -> 하위시스템이 어떤 것인지 알지 못해도, 또는 하위 시스템이 바뀌더라도 일관된 방법으로 접근할 수가 있음.

#### 스프링의 트랜잭션 서비스 추상화

- 스프링은 트랜잭션 기술의 공통점을 담은 추상화 기술을 제공함.
  ![](/img/img_12.png)

```java
class ex {
    public void upgradeLevels() throws Exception {
        PlatformTransactionManager transactionManager =
                new DataSourceTransactionManager(dataSource); //JDBC 트랜잭션 추상 오브젝트 생성

        //트랜잭션 시작
        TransactionStatus status =
                transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            List<User> users = userDao.getAll();
            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            transactionManager.commit(status); //트랜잭션 커밋
        } catch (Exception e) {
            transactionManager.rollback(status); //트랜잭션 커밋
            throw e;
        }
    }
}
```

##### PlatformTransactionManger

- 스프링이 제공하는 트랜잭션 경계설정 추상 인터페이스
- -> JDBC 로컬 트랜잭션을 이용한다면 DataSourceTransactionMAnager를 구현 클래스로 사용
- JDBC를 이용하는 경우엔 먼저 Connection을 생성하고 나서 트랜잭션을 시작했지만
- -> PlatformTransactionManager 에서는 트랜잭션을 가져오는 getTransaction() 메소드를 호출하기만 하면 됨
- -> 필요에 따라 트랜잭션 매니저가 DB 커넥션을 가져오는 작업도 같이 수행해주기 떄문
- -> 파라미터로 넘기는 DefaultTransactionDefinition 오브젝트는 트랜잭션에 대한 속성을 담고 있음

```
//트랜잭션 시작
TransactionStatus status =
    transactionManager.getTransaction(new DefaultTransactionDefinition());
```

- -> 파라미터로 넘기는 DefaultTransactionDefinition 오브젝트는 트랜잭션에 대한 속성을 담고 있음.
- -> 이렇게 시작된 트랜잭션은 TransactionStatus 타입의 변수에 저장됨
- -> TransactionStatus는 트랜잭션에 대한 조작이 필요할 때 PlatformTransactionManager 메소드의 파라미터로 전달.
- -> 트랜잭션 작업을 모두 수행한 후에는 트랜잭션을 만들 때 돌려받은 TransactionStatus 오브젝트를 파라미터로 해서 PlatfromTrasactionManager의 commit() 호출, 예외시
  rollback() 호출

#### 트랜잭션 기술 설정의 분리

- 트랜잭션 추상화 API를 적용한 UserService 코드를 JTA를 이용하는 글로벌 트랜잭션으로 변경하기
- DataSourceTransactionManager 구현 클래스를 -> JTATransactionManager로 바꿔 주기만 하면 됨
- JTATransactionManager는 주요 자바 서버에서 제공하는 JTA 정보를 JNDI를 통해 자동으로 인식하는 기능 갖고 있음
- 하지만 현재 어떤 트랜잭션 매니저 구현 클래스를 사용할지 UserService 코드가 알고 있는 것은 DI 원칙에 위배됨
- -> 그렇다면 DataSourceTransactionManager는 스프링 빈으로 등록하고 UserService가 DI 방식으로 사용하게 해야함.

##### 클래스를 스프링 빈으로 등록할 때 검토할 사항

- 어떤 클래스든 스프링의 빈으로 등록할 때 먼저 검토해야할 것은
- -> **싱글톤으로 만들어져 여러 스레드에서 동시에 사용해도 괜찮은가** 하는 점!
- 상태를 갖고 있고, 멀티스레드 환경에서 안전하지 않으면 문제가 발생..
- PlatformTranasctionManager의 구현 클래스는 싱글톤 사용 가능

```java
public class UserService {
    //..
    private PlatformTransactionManager transactionManager;

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    //..
}
```

- 트랜잭션을 JTA를 이용하는 것으로 변경하려면 빈 설정 변경해주기
- -> JTA는 애플리케이션 서버의 트랜잭션 서비스를 이용하기 때문에 DataSource도 서버가 제공해주는 것을 사용해야함..

## 서비스 추상화와 단일 책임 원칙

#### 수직, 수평 계층구조와 의존관계

- 애플리케이션 로직에 따른 수평적 구분이든, 로직과 기술이라는 수직적 구분이든 스프링의 DI를 통하여 결합도를 낮출 수 있었음.

#### 단일 책임 원칙

- 이런 적절한 분리가 가져오는 특징은 단일 책임 원칙
- 하나의 모듈은 한가지 책임을 가져야함 -> 하나의 모듈이 바뀌는 이유는 한 가지

#### 단일 책임 원칙의 장점

- 어떤 변경이 필요할 때 수정 대상이 명확해짐

## 메일 서비스 추상화

- 레벨이 업그레이드되는 사용자에게 안내 메일 발송 기능 추가하기

### JavaMail을 이용한 메일 발송 기능

- javax.mail 패키지에서 제공하는 자바의 이메일 클래스 사용

```java
class ex {
    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
        sendUpgradeEMail(user);
    }
}
```

```java
class ex {
    private void sendUpgradeEMail(User user) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.ksug.org");
        Session s = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(s);
        try {
            messgae.setFrom(new InternetAddress("useradmin@ksug.org"));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(user.getEmail()));
            message.setSubject("Upgrade 안내");
            message.setText("사용자님의 등급이 " + user.getLevel().name() + "로 업그레이드되었습니다.");
            Transport.send(message);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### JavaMail이 포함된 코드의 테스트
- 메일서버가 준비되어있지 않을 때 테스트는 어떻게?
- SMTP라는 표준 메일 발송 프로토콜로 메일 서버에 요청이 전달되기만 한다면 메일이 발송될 것이라고 믿고,
- 실제 메일 서버가 아닌 테스트용으로 따로 준비한 메일 서버를 사용해 테스트해도 좋다면
- -> 똑같은 원리를 UserService와 JavaMail사이에도 적용할 수 있음.
- -> JavaMail은 자바의 표준기술이므로, JavaMail API를 통해 요청이 들어간다는 보장만 있다면 테스트할떄마다 JavaMail을 직접 구동시킬 ㅣㅍㄹ요가 없음
- -> 개발중이거나 테스트를 수행할때난 JavaMail을 대신할 수 있으면서 JavaMail과 동일한 인터페이스를 갖는 코드가 동작하도록 만들어도 됨

### 테스트를 위한 서비스 추상화
- JavaMail과 동일한 인터페이스를 갖는 오브젝트를 만들어서 테스트
#### JavaMail을 이용한 ㅔㅌ스트의 문제점
- JavaMail의 핵심 API에는 DataSource처럼 인터페이스로 만들어져서 구현을 바꿀 수 있는게 없음.
- 메일 발송을 위해 가장 먼저 생성해야하는 javax.mail.Session 클래스의 사용방법 살펴보기
```
Session s = Session.getInstance(props, null);
```
- JavaMail에서는 Session 오브젝트를 만들어야만 메일 메시지를 생성할 수 있고, 메일을 전송할 수 있음.
- Session은 인터페이스가 아니고 클래스, 생성자가 pricvate으로 되어있어 직섭 생성도 불가능.
- -> 스태틱 팩토리 메소드를 이용해 오브젝트를 만드는 방법밖에 없음
- -> Session 클래스는 더 이상 상속이 불가능한 final 클래스.
- 메일메시지를 작성하는 MailMessage, 전송기능을 맡고 있는 Transport도 마찬가지.
- -> JavaMail처럼 테스트하기 힘든 구조인 API를 테스트하기 좋게 만드는 방법
- -> 트랜잭션을 적용하면서 살펴봤던 서비스 추상화를 적용하면 됨. 
- -> 스프링은 JavaMail에 대한 추상화 기능을 제공함.

#### 메일 발송 기능 추상화
- JavaMail의 서비스 추상화 인터페이스
```java
package org.springframework.mail;
//...
public interface MailSender{
    void send(SimpleMailMessage simpleMessage) throws MailException;
    void sned(SimpleMailMessgae[] simpleMessages) throws MailException;
}
```
- 위 인터페이스는 SimpleMailMessgae라는 인터페이스를 구현한 클래스에 담긴 메일메시지를 전송하는 메소드로만 구성되어있음. 
- -> 기본적으로 JavaMail을 사용해 메일발송기능을 제공하는 JavaMailSenderImple 클래스를 이용하면 됨

```java
class ex{
  private void sendUpgradeEMail(User user){
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("mail.server.com");
    SimpleMailMessage mailMessage = new SimpleMailMessage();
    mailMessage.setTo(user.getEmail());
    mailMessage.setSubject("Upgrade 안내");
    mailMessage.setText("사용자님의 등급이" + user.getLevel().name());
    mailSender.send(mailMessage);
  }
}
```
- 스프링의 MailSender를 이용한 메일 발송 메소드 
- 메시지는 MailMessage 인터페이스를 구현한 JavaMailSenederImple의 오브젝트 만들어 사용
- JavaMailSenderImpl은 내부적으로 JavaMail API를 이용해 메일을 전송해줌. 