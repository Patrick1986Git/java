package service;

import model.Student;
import repository.StudentRepository;
import utils.LoggerUtil;
import utils.Validator;
import utils.concurrent.AppExecutors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class StudentServiceImpl implements StudentService {

    private final StudentRepository repository;

    public StudentServiceImpl(StudentRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Student> create(Student student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Validator.requireNonNull(student, "student");
                Validator.requireNonBlank(student.getName(), "name");
                Validator.requireNonBlank(student.getSurname(), "surname");
                Validator.requireNonBlank(student.getUniversity(), "university");

                if (student.getYear() == null || student.getYear() <= 0)
                    student.setYear(1);

                student.setCreatedAt(LocalDate.now());
                student.setUpdatedAt(LocalDate.now());

                Student saved = repository.save(student);
                LoggerUtil.log(Level.INFO, "Created student: " + saved);
                return saved;
            } catch (Exception ex) {
                LoggerUtil.error("Error creating student", ex);
                throw new RuntimeException(ex);
            }
        }, AppExecutors.DB_EXECUTOR);
    }

    @Override
    public CompletableFuture<Optional<Student>> findById(Integer id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Validator.requireNonNull(id, "id");
                return repository.findById(id);
            } catch (Exception ex) {
                LoggerUtil.error("Error finding student", ex);
                throw new RuntimeException(ex);
            }
        }, AppExecutors.DB_EXECUTOR);
    }

    @Override
    public CompletableFuture<List<Student>> findAll(int page, int size, String sortBy, boolean asc) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findAll(page, size, sortBy, asc);
            } catch (Exception ex) {
                LoggerUtil.error("Error finding students", ex);
                throw new RuntimeException(ex);
            }
        }, AppExecutors.DB_EXECUTOR);
    }

    @Override
    public CompletableFuture<Student> update(Student student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Validator.requireNonNull(student, "student");
                Validator.requireNonNull(student.getId(), "student.id");
                Validator.requireNonBlank(student.getName(), "name");
                Validator.requireNonBlank(student.getSurname(), "surname");
                Validator.requireNonBlank(student.getUniversity(), "university");

                if (student.getYear() == null || student.getYear() <= 0)
                    student.setYear(1);

                student.setUpdatedAt(LocalDate.now());
                Student updated = repository.update(student);
                LoggerUtil.log(Level.INFO, "Updated student: " + updated);
                return updated;
            } catch (Exception ex) {
                LoggerUtil.error("Error updating student", ex);
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
                LoggerUtil.log(Level.INFO, "Deleted student id=" + id + "? " + deleted);
                return deleted;
            } catch (Exception ex) {
                LoggerUtil.error("Error deleting student", ex);
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
                LoggerUtil.error("Error counting students", ex);
                throw new RuntimeException(ex);
            }
        }, AppExecutors.DB_EXECUTOR);
    }
}
