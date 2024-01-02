package no.nav.sak.repository;

public class JunitDatabase {
    private static Database database;

    public static Database get() {
        if (database == null) {
            database = new Database(JunitDataSource.get());
        }
        return database;
    }
}
