package service;

import model.Person;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PersonService {
    CompletableFuture<Person> create(Person person);
    CompletableFuture<Optional<Person>> findById(Integer id);
    CompletableFuture<List<Person>> findAll(int page, int size, String sortBy, boolean asc);
    CompletableFuture<List<Person>> findAll();
    CompletableFuture<Person> update(Person person);
    CompletableFuture<Boolean> deleteById(Integer id);
    CompletableFuture<Long> count();
}



