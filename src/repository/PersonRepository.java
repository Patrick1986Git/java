package repository;

import model.Person;

public interface PersonRepository extends BaseRepository<Person, Integer> {
    // Możesz dodać specyficzne metody np. searchByName, findBySurname, statystyki itp.
}