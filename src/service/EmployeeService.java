package service;

import model.Employee;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EmployeeService {
    CompletableFuture<Employee> create(Employee employee);
    CompletableFuture<Optional<Employee>> findById(Integer id);
    CompletableFuture<List<Employee>> findAll(int page, int size, String sortBy, boolean asc);
    CompletableFuture<List<Employee>> findAll();
    CompletableFuture<Employee> update(Employee employee);
    CompletableFuture<Boolean> deleteById(Integer id);
    CompletableFuture<Long> count();
}


