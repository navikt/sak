package no.nav.sak.infrastruktur;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;


@Slf4j
public class TransactionManager {

    private final Database database;

    public TransactionManager(Database database) {
        this.database = database;
    }

    public void doInTransaction(Runnable operation) {
        ThreadLocal<Connection> threadConnection = database.getThreadConnection();
        if (threadConnection.get() != null) {
            log.debug("Kobler til eksisterende transaksjon");
            operation.run();
        } else {
            log.debug("Oppretter transaksjon");
            try (Connection connection = database.getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                threadConnection.set(connection);
                executeTx(operation, connection);
            } catch (SQLException e) {
                throw new DatabaseException(e);
            } finally {
                threadConnection.remove();
            }
        }
    }

    private void executeTx(Runnable operation, Connection connection) throws SQLException {
        try {
            operation.run();
            log.debug("Transaksjon gjennomført, committer");
            connection.commit();
        } catch (RuntimeException e) {
            log.debug("Transaksjon feilet, ruller tilbake");
            connection.rollback();
            throw e;
        }
    }
}
