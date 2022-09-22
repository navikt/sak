package no.nav.sak.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

@Slf4j
@Component
public class Database {

    private final DataSource dataSource;
    private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    @Autowired
    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Long insert(String sql, Object... parameters) {
        return doWithConnection(conn -> {
            log.debug("Utfører: {}, med parametre: {}", sql, parameters);
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql, new String[]{"id"})) {
                int index = 1;
                for (Object param : parameters) {
                    preparedStatement.setObject(index++, param);
                }
                preparedStatement.executeUpdate();

                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new IllegalArgumentException(String.format("Fant ingen generert id for sql: %s. Har du glemt å benytte sekvens?",
                        sql));
                }
            } catch (SQLException e) {
                log.error("SQLException for: {} med params: {}", sql, parameters, e);
                throw new DatabaseException(e);
            }
        });
    }

    public <T> Optional<T> queryForSingle(String query, RowMapper<T> mapper, Object... parameters) {
        return executeDbOperation(query, asList(parameters), stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                return mapSingleRow(rs, mapper);
            }
        });
    }

    public <T> List<T> queryForList(String query, List<Object> parameters, RowMapper<T> mapper) {
        return executeDbOperation(query, parameters, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                Row row = new Row(rs);
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.run(row));
                }
                return result;
            }
        });
    }

    public void execute(String query, Object... parameters) {
        executeDbOperation(query, asList(parameters), PreparedStatement::executeUpdate);
    }


    public ThreadLocal<Connection> getThreadConnection() {
        return threadConnection;
    }


    private <T> T doWithConnection(ConnectionCallback<T> object) {
        if (threadConnection.get() != null) {
            return object.run(threadConnection.get());
        }
        try (Connection conn = dataSource.getConnection()) {
            return object.run(conn);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private <T> T executeDbOperation(String query, Collection<Object> parameters,
                                     StatementCallback<T> statementCallback) {
        return doWithConnection(conn -> {
            log.debug("Kjører: {} med parametre {}", query, parameters);
            try (PreparedStatement prepareStatement = conn.prepareStatement(query)) {
                int index = 1;
                for (Object object : parameters) {
                    prepareStatement.setObject(index++, object);
                }
                return statementCallback.run(prepareStatement);
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }
        });
    }

    private <T> Optional<T> mapSingleRow(ResultSet rs, RowMapper<T> mapper) throws SQLException {
        if (!rs.next()) {
            return Optional.empty();
        }
        T result = mapper.run(new Row(rs));
        if (rs.next()) {
            throw new IllegalArgumentException("Duplikat");
        }
        return Optional.of(result);
    }

    private interface StatementCallback<T> {
        T run(PreparedStatement stmt) throws SQLException;
    }

    private interface ConnectionCallback<T> {
        T run(Connection conn);
    }

    public interface RowMapper<T> {
        T run(Row row) throws SQLException;
    }


    public static class Row {

        private final ResultSet rs;

        Row(ResultSet rs) {
            this.rs = rs;
        }

        public Long getLong(String columnName) throws SQLException {
            return rs.getLong(columnName);
        }

        public String getString(String columnName) throws SQLException {
            return rs.getString(columnName);
        }

        public LocalDateTime getLocalDateTime(String columnName) throws SQLException {
            return rs.getTimestamp(columnName).toLocalDateTime();
        }
    }
}

