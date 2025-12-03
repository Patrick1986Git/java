package repository;

import java.sql.Connection;

/**
 * Klasa bazowa dla implementacji JDBC. Trzyma Connection i wspólne utilsy.
 * Konkretne repozytoria wykonują mapowanie z ResultSet -> encja.
 */
public abstract class JdbcRepository<T, ID> implements BaseRepository<T, ID> {
    protected final Connection connection;

    protected JdbcRepository(Connection connection) {
        this.connection = connection;
    }

    // Możesz dodać tutaj wspólne metody helper (closeQuietly, transakcje, itp.)
}