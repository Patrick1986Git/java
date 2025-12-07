package repository;

import model.Role;
import model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UserRepositoryImpl {
	private final Connection connection;

	public UserRepositoryImpl(Connection connection) {
		this.connection = connection;
	}

	public User save(User user) throws SQLException {
		String sql = "INSERT INTO users (username, password_hash, salt, enabled, must_change_password, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, user.getUsername());
			ps.setBytes(2, user.getPasswordHash());
			ps.setBytes(3, user.getSalt());
			ps.setBoolean(4, user.isEnabled());
			ps.setBoolean(5, user.isMustChangePassword()); // <-- must_change_password
			ps.setDate(6,
					user.getCreatedAt() == null ? Date.valueOf(LocalDate.now()) : Date.valueOf(user.getCreatedAt()));
			ps.setDate(7,
					user.getUpdatedAt() == null ? Date.valueOf(LocalDate.now()) : Date.valueOf(user.getUpdatedAt()));
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next())
					user.setId(rs.getInt(1));
			}
		}

		// assign roles if any
		if (user.getRoles() != null) {
			for (Role r : user.getRoles()) {
				assignRole(user.getId(), r.getId());
			}
		}
		return user;
	}

	public Optional<User> findByUsername(String username) throws SQLException {
		String sql = "SELECT * FROM users WHERE username = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					User u = mapRow(rs);
					u.setRoles(loadRoles(u.getId()));
					return Optional.of(u);
				}
			}
		}
		return Optional.empty();
	}

	private Set<Role> loadRoles(int userId) throws SQLException {
		String sql = "SELECT r.id, r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				Set<Role> roles = new HashSet<>();
				while (rs.next())
					roles.add(new Role(rs.getInt("id"), rs.getString("name")));
				return roles;
			}
		}
	}

	private User mapRow(ResultSet rs) throws SQLException {
		User u = new User();
		u.setId(rs.getInt("id"));
		u.setUsername(rs.getString("username"));
		u.setPasswordHash(rs.getBytes("password_hash"));
		u.setSalt(rs.getBytes("salt"));
		u.setEnabled(rs.getBoolean("enabled"));
		try {
			u.setMustChangePassword(rs.getBoolean("must_change_password"));
		} catch (SQLException ex) {
			u.setMustChangePassword(false);
		}
		Date ca = rs.getDate("created_at");
		if (ca != null)
			u.setCreatedAt(ca.toLocalDate());
		Date ua = rs.getDate("updated_at");
		if (ua != null)
			u.setUpdatedAt(ua.toLocalDate());
		return u;
	}

	public void updatePasswordByUsername(String username, byte[] passwordHash, byte[] salt, boolean mustChange)
			throws SQLException {
		String sql = "UPDATE users SET password_hash = ?, salt = ?, must_change_password = ?, updated_at = ? WHERE username = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setBytes(1, passwordHash);
			ps.setBytes(2, salt);
			ps.setBoolean(3, mustChange);
			ps.setDate(4, Date.valueOf(LocalDate.now()));
			ps.setString(5, username);

			// log przed wykonaniem (info)
			utils.LoggerUtil.info("Updating password for user: " + username);

			int updated = ps.executeUpdate();

			if (updated > 0) {
				utils.LoggerUtil.log(java.util.logging.Level.INFO,
						"Password update affected " + updated + " row(s) for user=" + username);
			} else {
				utils.LoggerUtil.log(java.util.logging.Level.WARNING,
						"Password update affected 0 rows for user=" + username);
			}
		} catch (SQLException ex) {
			utils.LoggerUtil.error("Failed to update password for user: " + username, ex);
			throw ex;
		}
	}

	public void assignRole(Integer userId, Integer roleId) throws SQLException {
		String sql = "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, userId);
			ps.setInt(2, roleId);
			ps.executeUpdate();
		}
	}

	public boolean deleteById(Integer id) throws SQLException {
		String sql = "DELETE FROM users WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, id);
			return ps.executeUpdate() > 0;
		}
	}
}
