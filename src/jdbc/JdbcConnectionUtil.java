package jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Klasa narzędziowa do zarządzania połączeniem JDBC z bazą MySQL. Automatycznie
 * inicjalizuje schemat bazy, jeśli nie istnieje.
 */
public final class JdbcConnectionUtil {

	private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/enterprise?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";
	private static final String DEFAULT_USER = "enterprise_user";
	private static final String DEFAULT_PASSWORD = "enterprise_pass";

	private static Connection connection;

	private JdbcConnectionUtil() {
	}

	/**
	 * Zwraca współdzielone połączenie z bazą danych. Tworzy je tylko raz (singleton
	 * pattern).
	 */
	public static synchronized Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			try {
				// W nowszych wersjach JDBC nie jest to wymagane, ale dla pewności:
				Class.forName("com.mysql.cj.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				throw new SQLException("Brak sterownika MySQL JDBC!", e);
			}

			connection = DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
		//	initializeSchema(connection);
		}
		return connection;
	}

	/**
	 * Inicjalizuje strukturę tabeli `persons`, jeśli nie istnieje.
	 */
	private static void initializeSchema(Connection conn) {
		try (Statement st = conn.createStatement()) {
			String create = """
					CREATE TABLE IF NOT EXISTS persons (
					    id INT AUTO_INCREMENT PRIMARY KEY,
					    name VARCHAR(100) NOT NULL,
					    surname VARCHAR(100) NOT NULL,
					    age INT,
					    date_of_birth DATE,
					    start_date DATE,
					    salary DOUBLE,
					    position VARCHAR(100),
					    university VARCHAR(200),
					    year INT
					)
					""";
			st.execute(create);
		} catch (SQLException e) {
			throw new RuntimeException("Failed to initialize DB schema", e);
		}
	}

	/**
	 * Zamyka połączenie z bazą danych, jeśli jest otwarte.
	 */
	public static synchronized void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ignored) {
			}
		}
	}
}
