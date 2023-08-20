package org.user.dao;

import org.user.domain.User;

import javax.sql.DataSource;
import java.sql.*;

public abstract class UserDao {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(User user) throws SQLException {
        Connection c = dataSource.getConnection();
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
            if(c != null){
                try{
                    c.close();
                }catch (SQLException e){
                }
            }
        }
    }

   abstract protected PreparedStatement makeStatemet(Connection c) throws SQLException;

    public int getCount() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try{
            c = dataSource.getConnection();
            ps = c.prepareStatement("select count(*) from users");

            rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }catch (SQLException e){
            throw e;
        }finally {
            if(rs != null){
                try{
                    rs.close();
                }catch (SQLException e){
                }
            }
            if( ps != null){
                try{
                    ps.close();
                }catch (SQLException e){
                }
            }
            if(c != null){
                try{
                    c.close();
                }catch (SQLException e){
                }
            }
        }
    }
}
