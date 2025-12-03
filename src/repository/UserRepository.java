package repository;

import model.User;

import java.util.Optional;

public interface UserRepository {
	User save(User user) throws Exception;
	Optional<User> findById(Integer id) throws Exception;
	Optional<User> findByUsername(String username) throws Exception;
	boolean deleteById(Integer id) throws Exception;
	void assignRole(Integer userId, Integer roleId) throws Exception;
}
