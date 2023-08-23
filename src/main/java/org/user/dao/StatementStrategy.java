package org.user.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface StatementStrategy { //콜백 패턴을 위한 인터페이스 -> 보통 단일 메소드를 가진 인터페이스
    PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
