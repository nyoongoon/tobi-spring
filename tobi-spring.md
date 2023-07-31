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