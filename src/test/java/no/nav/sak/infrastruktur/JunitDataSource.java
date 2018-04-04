package no.nav.sak.infrastruktur;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class JunitDataSource {
    private static JdbcDataSource dataSource;

    public static DataSource get() {
        if (dataSource == null) {
            dataSource = new JdbcDataSource();
            dataSource.setUser("sa");
            dataSource.setPassword("");
            dataSource.setURL("jdbc:h2:mem:sak;Mode=Oracle;DB_CLOSE_DELAY=-1");
        }
        return dataSource;
    }
}
