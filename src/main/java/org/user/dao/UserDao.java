package org.user.dao;

import org.user.domain.User;

import javax.sql.DataSource;
import java.sql.*;

public class UserDao {
    private JdbcContext jdbcContext;

    public void setJdbcContext(JdbcContext jdbcContext) {
        this.jdbcContext = jdbcContext;
    }

    public void add(User user) throws SQLException {
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

    public User get(String id) throws SQLException {
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(
                "select * from users where id = ?");
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();

        User user = null;

        if (rs.next()) { // 조회결과가 null이라도 코드가 진행될 수 있게 수정
            user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
        }

        rs.close();
        ps.close();
        c.close();

        if (user == null) throw new SQLDataException();

        return user;
    }

    public void deleteAll() throws SQLException { // 전략패턴의 클라이언트가 된 메소드
        jdbcContext.workWithStatementStrategy();
        jdbcContextWithStatementStrategy(
                new StatementStrategy() { //익명 내부 클래스 !!! -> 구현하는 인터페이스를 생성자처럼 이용해서 오브젝트 만듬
                    @Override
                    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                        return c.prepareStatement("delete from users");
                    }
                });
    }

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
