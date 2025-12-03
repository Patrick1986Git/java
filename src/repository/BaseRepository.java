package repository;

import java.util.List;
import java.util.Optional;

/**
 * Generyczny interfejs CRUD + prosta paginacja i sortowanie.
 * T - encja, ID - typ klucza.
 */
public interface BaseRepository<T, ID> {
    T save(T entity) throws Exception;
    Optional<T> findById(ID id) throws Exception;
    List<T> findAll() throws Exception;
    List<T> findAll(int page, int size, String sortBy, boolean asc) throws Exception; // strona od 0
    T update(T entity) throws Exception;
    boolean deleteById(ID id) throws Exception;
    long count() throws Exception;
}