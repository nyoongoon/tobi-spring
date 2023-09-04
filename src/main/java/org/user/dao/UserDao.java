package org.user.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.user.domain.User;

import javax.sql.DataSource;
import java.sql.*;

public class UserDao {
    private JdbcContext jdbcContext;
    public JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
//        this.jdbcContext = new JdbcContext();
//        this.jdbcContext.setDataSource(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void add(User user) throws SQLException {
//        jdbcContext.workWithStatementStrategy(
//                new StatementStrategy() { //익명 내부 클래스 !!! -> 구현하는 인터페이스를 생성자처럼 이용해서 오브젝트 만듬
//            @Override
//            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
//                PreparedStatement ps =
//                        c.prepareStatement("insert into users(id, name, password) values(?,?,?)");
//                ps.setString(1, user.getId());
//                ps.setString(2, user.getName());
//                ps.setString(3, user.getPassword()); //로컬 클래스에서 외부 메소드 로컬 변수에 직접 접근 가능 !!! (final일 경우)
//                return ps;
//            }
//        });

        this.jdbcTemplate.update("insert into users(id, name, password) values(?,?,?)",
                user.getId(), user.getName(), user.getPassword());
    }

//    public User get(String id) throws SQLException {
//        jdbcContext.workWithStatementStrategy(
//                new StatementStrategy() {
//                    @Override
//                    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
//                        return null;
//                    }
//                }
//        );
//
//        Connection c = dataSource.getConnection();
//        PreparedStatement ps = c.prepareStatement(
//                "select * from users where id = ?");
//        ps.setString(1, id);
//
//        ResultSet rs = ps.executeQuery();
//
//        User user = null;
//
//        if (rs.next()) { // 조회결과가 null이라도 코드가 진행될 수 있게 수정
//            user = new User();
//            user.setId(rs.getString("id"));
//            user.setName(rs.getString("name"));
//            user.setPassword(rs.getString("password"));
//        }
//
//        rs.close();
//        ps.close();
//        c.close();
//
//        if (user == null) throw new SQLDataException();
//
//        return user;
//    }

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


    public void deleteAll() throws SQLException { // 전략패턴의 클라이언트가 된 메소드
//       jdbcContext.executeSql("delete from users");
//        this.jdbcTemplate.update(
//                new PreparedStatementCreator() {
//                    @Override
//                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
//                        return con.prepareStatement("delete from users");
//                    }
//                }
//        );
        this.jdbcTemplate.update("delete from users");
    }

//    public int getCount() throws SQLException {
//        Connection c = null;
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//
//        try {
//            c = dataSource.getConnection();
//            ps = c.prepareStatement("select count(*) from users");
//
//            rs = ps.executeQuery();
//            rs.next();
//            return rs.getInt(1);
//        } catch (SQLException e) {
//            throw e;
//        } finally {
//            if (rs != null) {
//                try {
//                    rs.close();
//                } catch (SQLException e) {
//                }
//            }
//            if (ps != null) {
//                try {
//                    ps.close();
//                } catch (SQLException e) {
//                }
//            }
//            if (c != null) {
//                try {
//                    c.close();
//                } catch (SQLException e) {
//                }
//            }
//        }
//    }

    public int getCount() {
//        return this.jdbcTemplate.query(new PreparedStatementCreator() {
//            @Override
//            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
//                return con.prepareStatement("select count(*) from users");
//            }
//        }, new ResultSetExtractor<Integer>() {
//            public Integer extractData(ResultSet rs) throws SQLException {
//                rs.next();
//                return rs.getInt(1);
//
//            }
//        });
        return this.jdbcTemplate.queryForInt("select count(*) from users");
    }


}
