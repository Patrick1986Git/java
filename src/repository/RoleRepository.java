package repository;

public interface RoleRepository {
    /**
     * Zwraca id roli o danej nazwie lub null jeżeli nie istnieje.
     */
    Integer findIdByName(String name) throws Exception;

    /**
     * Zwraca id roli - jeśli nie istnieje to tworzy ją i zwraca nowe id.
     */
    Integer findOrCreateRole(String name) throws Exception;
}
