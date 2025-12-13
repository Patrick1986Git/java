package service;

import model.Employee;
import repository.EmployeeRepository;
import security.AuthManager;
import utils.LoggerUtil;
import utils.Validator;
import utils.concurrent.AppExecutors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class EmployeeServiceImpl implements EmployeeService {
	private final EmployeeRepository repository;

	public EmployeeServiceImpl(EmployeeRepository repository) {
		this.repository = repository;
	}

	@Override
	public CompletableFuture<Employee> create(Employee employee) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(employee, "employee");
				Validator.requireNonBlank(employee.getName(), "name");
				Validator.requireNonBlank(employee.getSurname(), "surname");
				if (employee.getSalary() == null || employee.getSalary() < 0)
					throw new IllegalArgumentException("Salary must be non-negative");

				employee.setCreatedAt(LocalDate.now());
				employee.setUpdatedAt(LocalDate.now());

				Employee saved = repository.save(employee);
				LoggerUtil.log(java.util.logging.Level.INFO, "Created employee: id=" + saved.getId() + " by user="
						+ AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("CREATE_EMPLOYEE", "employee:" + saved.getId(),
						"position=" + saved.getPosition() + " salary=" + saved.getSalary());
				return saved;
			} catch (Exception e) {
				LoggerUtil.error("Error creating employee", e);
				throw new RuntimeException(e);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Optional<Employee>> findById(Integer id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(id, "id");
				return repository.findById(id);
			} catch (Exception e) {
				LoggerUtil.error("Error finding employee", e);
				throw new RuntimeException(e);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<List<Employee>> findAll(int page, int size, String sortBy, boolean asc) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repository.findAll(page, size, sortBy, asc);
			} catch (Exception e) {
				LoggerUtil.error("Error fetching employees", e);
				throw new RuntimeException(e);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<List<Employee>> findAll() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repository.findAll();
			} catch (Exception ex) {
				LoggerUtil.error("Error finding all employees", ex);
				throw new RuntimeException(ex);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Employee> update(Employee employee) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Validator.requireNonNull(employee, "employee");
				Validator.requireNonNull(employee.getId(), "employee.id");
				Validator.requireNonBlank(employee.getName(), "name");
				Validator.requireNonBlank(employee.getSurname(), "surname");

				employee.setUpdatedAt(LocalDate.now());

				Employee updated = repository.update(employee);
				LoggerUtil.log(java.util.logging.Level.INFO, "Updated employee: id=" + updated.getId() + " by user="
						+ AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("UPDATE_EMPLOYEE", "employee:" + updated.getId(),
						"position=" + updated.getPosition() + " salary=" + updated.getSalary());
				return updated;
			} catch (Exception e) {
				LoggerUtil.error("Error updating employee", e);
				throw new RuntimeException(e);
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
						"Deleted employee id=" + id + " by user=" + AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("DELETE_EMPLOYEE", "employee:" + id, "deleted=" + deleted);
				return deleted;
			} catch (Exception e) {
				LoggerUtil.error("Error deleting employee", e);
				throw new RuntimeException(e);
			}
		}, AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Long> count() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repository.count();
			} catch (Exception e) {
				LoggerUtil.error("Error counting employees", e);
				throw new RuntimeException(e);
			}
		}, AppExecutors.DB_EXECUTOR);
	}
}
