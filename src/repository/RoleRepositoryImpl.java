package repository;

import java.sql.*;

/**
 * Proste repozytorium ról (roles).
 */
public class RoleRepositoryImpl implements RoleRepository {

	private final Connection connection;

	public RoleRepositoryImpl(Connection connection) {
		this.connection = connection;
	}

	@Override
	public Integer findIdByName(String name) throws SQLException {
		String sql = "SELECT id FROM roles WHERE name = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("id");
			}
		}
		return null;
	}

	@Override
	public Integer findOrCreateRole(String name) throws SQLException {
		// spróbuj znaleźć
		Integer id = findIdByName(name);
		if (id != null)
			return id;

		// utwórz nową rolę (zwróć id)
		String insert = "INSERT INTO roles (name) VALUES (?)";
		try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, name);
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next())
					return rs.getInt(1);
			}
		}
		// jeśli tu nie zwrócono id, spróbuj jeszcze raz pobrać (konkurencja)
		return findIdByName(name);
	}
}
