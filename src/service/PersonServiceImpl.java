package service;

import model.Person;
import repository.PersonRepository;
import security.AuthManager;
import utils.LoggerUtil;
import utils.Validator;
import utils.concurrent.AppExecutors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PersonServiceImpl implements PersonService {

	private final PersonRepository repository;

	public PersonServiceImpl(PersonRepository repository) {
		this.repository = repository;
	}

	@Override
	public CompletableFuture<Person> create(Person person) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(person, "person");
				Validator.requireNonBlank(person.getName(), "name");
				Validator.requireNonBlank(person.getSurname(), "surname");

				if (person.getAge() == null)
					person.setAge(0);
				person.setCreatedAt(LocalDate.now());
				person.setUpdatedAt(LocalDate.now());

				Person saved = repository.save(person);
				LoggerUtil.log(java.util.logging.Level.INFO, "Created person: id=" + saved.getId() + " by user="
						+ AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("CREATE_PERSON", "person:" + saved.getId(),
						"name=" + saved.getName() + " surname=" + saved.getSurname());
				return saved;
			} catch (Exception ex) {
				LoggerUtil.error("Error creating person", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Optional<Person>> findById(Integer id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(id, "id");
				return repository.findById(id);
			} catch (Exception ex) {
				LoggerUtil.error("Error finding person by id", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<List<Person>> findAll(int page, int size, String sortBy, boolean asc) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repository.findAll(page, size, sortBy, asc);
			} catch (Exception ex) {
				LoggerUtil.error("Error finding persons", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<List<Person>> findAll() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repository.findAll();
			} catch (Exception ex) {
				LoggerUtil.error("Error finding all persons", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Person> update(Person person) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(person, "person");
				Validator.requireNonNull(person.getId(), "person.id");
				Validator.requireNonBlank(person.getName(), "name");
				Validator.requireNonBlank(person.getSurname(), "surname");

				person.setUpdatedAt(LocalDate.now());

				Person updated = repository.update(person);
				LoggerUtil.log(java.util.logging.Level.INFO, "Updated person: id=" + updated.getId() + " by user="
						+ AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("UPDATE_PERSON", "person:" + updated.getId(),
						"name=" + updated.getName() + " surname=" + updated.getSurname());
				return updated;
			} catch (Exception ex) {
				LoggerUtil.error("Error updating person", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Boolean> deleteById(Integer id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(id, "id");
				boolean deleted = repository.deleteById(id);
				LoggerUtil.log(java.util.logging.Level.INFO,
						"Deleted person id=" + id + " by user=" + AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("DELETE_PERSON", "person:" + id, "deleted=" + deleted);
				return deleted;
			} catch (Exception ex) {
				LoggerUtil.error("Error deleting person", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Long> count() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repository.count();
			} catch (Exception ex) {
				LoggerUtil.error("Error counting persons", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}
}
