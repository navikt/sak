package no.nav.sak.infrastruktur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;


public class TransactionManager {
    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

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
