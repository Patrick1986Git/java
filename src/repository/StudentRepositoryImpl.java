package repository;

import model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentRepositoryImpl extends JdbcRepository<Student, Integer> implements StudentRepository {

	public StudentRepositoryImpl(Connection connection) {
		super(connection);
	}

	@Override
	public Student save(Student entity) throws SQLException {
		String sql = "INSERT INTO persons (name, surname, age, university, year, date_of_birth, start_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, entity.getName());
			ps.setString(2, entity.getSurname());
			if (entity.getAge() != null)
				ps.setInt(3, entity.getAge());
			else
				ps.setNull(3, Types.INTEGER);
			ps.setString(4, entity.getUniversity());
			if (entity.getYear() != null)
				ps.setInt(5, entity.getYear());
			else
				ps.setNull(5, Types.INTEGER);
			if (entity.getDateOfBirth() != null)
				ps.setDate(6, Date.valueOf(entity.getDateOfBirth()));
			else
				ps.setNull(6, Types.DATE);
			if (entity.getStartDate() != null)
				ps.setDate(7, Date.valueOf(entity.getStartDate()));
			else
				ps.setNull(7, Types.DATE);

			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next())
					entity.setId(rs.getInt(1));
			}
			return entity;
		}
	}

	@Override
	public Optional<Student> findById(Integer id) throws SQLException {
		String sql = "SELECT * FROM persons WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return Optional.of(mapRowToStudent(rs));
			}
		}
		return Optional.empty();
	}

	@Override
	public List<Student> findAll() throws SQLException {
		String sql = "SELECT * FROM persons WHERE university IS NOT NULL";
		try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			List<Student> list = new ArrayList<>();
			while (rs.next())
				list.add(mapRowToStudent(rs));
			return list;
		}
	}

	@Override
	public List<Student> findAll(int page, int size, String sortBy, boolean asc) throws SQLException {
		String order = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
		String direction = asc ? "ASC" : "DESC";
		int offset = page * size;
		String sql = String.format("SELECT * FROM persons WHERE university IS NOT NULL ORDER BY %s %s LIMIT ? OFFSET ?",
				order, direction);
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, size);
			ps.setInt(2, offset);
			try (ResultSet rs = ps.executeQuery()) {
				List<Student> list = new ArrayList<>();
				while (rs.next())
					list.add(mapRowToStudent(rs));
				return list;
			}
		}
	}

	@Override
	public Student update(Student entity) throws SQLException {
		String sql = "UPDATE persons SET name = ?, surname = ?, age = ?, university = ?, year = ?, date_of_birth = ?, start_date = ? WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, entity.getName());
			ps.setString(2, entity.getSurname());
			if (entity.getAge() != null)
				ps.setInt(3, entity.getAge());
			else
				ps.setNull(3, Types.INTEGER);
			ps.setString(4, entity.getUniversity());
			if (entity.getYear() != null)
				ps.setInt(5, entity.getYear());
			else
				ps.setNull(5, Types.INTEGER);
			if (entity.getDateOfBirth() != null)
				ps.setDate(6, Date.valueOf(entity.getDateOfBirth()));
			else
				ps.setNull(6, Types.DATE);
			if (entity.getStartDate() != null)
				ps.setDate(7, Date.valueOf(entity.getStartDate()));
			else
				ps.setNull(7, Types.DATE);
			ps.setInt(8, entity.getId());
			ps.executeUpdate();
			return entity;
		}
	}

	@Override
	public boolean deleteById(Integer id) throws SQLException {
		String sql = "DELETE FROM persons WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, id);
			return ps.executeUpdate() > 0;
		}
	}

	@Override
	public long count() throws SQLException {
		String sql = "SELECT COUNT(*) FROM persons WHERE university IS NOT NULL";
		try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				return rs.getLong(1);
			return 0L;
		}
	}

	protected Student mapRowToStudent(ResultSet rs) throws SQLException {
		return new Student(rs.getInt("id"), rs.getString("name"), rs.getString("surname"), rs.getInt("age"),
				rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null,
				rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
				rs.getString("university"), rs.getObject("year") == null ? 1 : rs.getInt("year"));
	}
}