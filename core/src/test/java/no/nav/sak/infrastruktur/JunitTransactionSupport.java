package no.nav.sak.infrastruktur;

import java.sql.Connection;
import java.sql.SQLException;


public class JunitTransactionSupport {
    private final Database database;

    public JunitTransactionSupport(Database database) {
        this.database = database;
    }

    public void initTransaction() throws SQLException {
        ThreadLocal<Connection> threadConnection = database.getThreadConnection();
        Connection connection = database.getDataSource().getConnection();
        connection.setAutoCommit(false);
        threadConnection.set(connection);
    }

    public void rollback() throws SQLException {
        Connection connection = database.getThreadConnection().get();
        connection.rollback();
        database.getThreadConnection().remove();
    }
}

