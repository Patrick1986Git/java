package service;

import model.Student;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StudentService {
    CompletableFuture<Student> create(Student student);
    CompletableFuture<Optional<Student>> findById(Integer id);
    CompletableFuture<List<Student>> findAll(int page, int size, String sortBy, boolean asc);
    CompletableFuture<List<Student>> findAll();
    CompletableFuture<Student> update(Student student);
    CompletableFuture<Boolean> deleteById(Integer id);
    CompletableFuture<Long> count();
}
