package repository;

import model.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Prosta implementacja JDBC dla Person z podstawowymi operacjami. Zakładamy
 * tabelę `persons` zgodnie z przykładem: id INT AUTO_INCREMENT PRIMARY KEY,
 * name, surname, age, salary, university
 */
public class PersonRepositoryImpl extends JdbcRepository<Person, Integer> implements PersonRepository {

	public PersonRepositoryImpl(Connection connection) {
		super(connection);
	}

	@Override
	public Person save(Person entity) throws SQLException {
		String sql = "INSERT INTO persons (name, surname, age, date_of_birth, start_date) VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, entity.getName());
			ps.setString(2, entity.getSurname());
			if (entity.getAge() != null)
				ps.setInt(3, entity.getAge());
			else
				ps.setNull(3, Types.INTEGER);
			if (entity.getDateOfBirth() != null)
				ps.setDate(4, Date.valueOf(entity.getDateOfBirth()));
			else
				ps.setNull(4, Types.DATE);
			if (entity.getStartDate() != null)
				ps.setDate(5, Date.valueOf(entity.getStartDate()));
			else
				ps.setNull(5, Types.DATE);

			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next())
					entity.setId(rs.getInt(1));
			}
			return entity;
		}
	}

	@Override
	public Optional<Person> findById(Integer id) throws SQLException {
		String sql = "SELECT * FROM persons WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return Optional.of(mapRowToPerson(rs));
			}
		}
		return Optional.empty();
	}

	@Override
	public List<Person> findAll() throws SQLException {
		String sql = "SELECT * FROM persons";
		try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			List<Person> list = new ArrayList<>();
			while (rs.next())
				list.add(mapRowToPerson(rs));
			return list;
		}
	}

	@Override
	public List<Person> findAll(int page, int size, String sortBy, boolean asc) throws SQLException {
		String order = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
		String direction = asc ? "ASC" : "DESC";
		int offset = page * size;
		String sql = String.format("SELECT * FROM persons ORDER BY %s %s LIMIT ? OFFSET ?", order, direction);
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, size);
			ps.setInt(2, offset);
			try (ResultSet rs = ps.executeQuery()) {
				List<Person> list = new ArrayList<>();
				while (rs.next())
					list.add(mapRowToPerson(rs));
				return list;
			}
		}
	}

	@Override
	public Person update(Person entity) throws SQLException {
		String sql = "UPDATE persons SET name = ?, surname = ?, age = ?, date_of_birth = ?, start_date = ? WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, entity.getName());
			ps.setString(2, entity.getSurname());
			if (entity.getAge() != null)
				ps.setInt(3, entity.getAge());
			else
				ps.setNull(3, Types.INTEGER);
			if (entity.getDateOfBirth() != null)
				ps.setDate(4, Date.valueOf(entity.getDateOfBirth()));
			else
				ps.setNull(4, Types.DATE);
			if (entity.getStartDate() != null)
				ps.setDate(5, Date.valueOf(entity.getStartDate()));
			else
				ps.setNull(5, Types.DATE);
			ps.setInt(6, entity.getId());
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
		String sql = "SELECT COUNT(*) FROM persons";
		try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getLong(1) : 0L;
		}
	}

	// Proste mapowanie do Person (bez mapowania pola salary/university do konkretnych podtypów)
	protected Person mapRowToPerson(ResultSet rs) throws SQLException {
		Person p = new Person(rs.getInt("id"), rs.getString("name"), rs.getString("surname"), rs.getInt("age"),
				rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null,
				rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null) {
		};
		return p;
	}
}