package repository;

import model.Employee;

public interface EmployeeRepository extends BaseRepository<Employee, Integer> {
	// Możesz dodać metody specyficzne (np. findByPosition, findBySalaryRange)
}