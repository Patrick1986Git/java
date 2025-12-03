package repository;

import model.Student;

public interface StudentRepository extends BaseRepository<Student, Integer> {
    // Możesz dodać findByUniversity, findByYear itp.
}