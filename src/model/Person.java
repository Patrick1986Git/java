package model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstrakcyjna encja Person. Zawiera pola wspólne dla wszystkich osób
 * (Employee, Student).
 */
public abstract class Person {
    private Integer id;
    private String name;
    private String surname;
    private Integer age;
    private LocalDate dateOfBirth;
    private LocalDate startDate;

    //Pola systemowe (do logowania zmian)
    private LocalDate createdAt;
    private LocalDate updatedAt;

    protected Person() {
    }

    protected Person(Integer id, String name, String surname, Integer age,
                     LocalDate dateOfBirth, LocalDate startDate) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
        this.surname = surname == null ? "" : surname.trim();
        this.age = age;
        this.dateOfBirth = dateOfBirth;
        this.startDate = startDate;
    }

    // Gettery i settery

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname == null ? "" : surname.trim();
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    // Gettery/settery dla pól systemowych
    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", age=" + age +
                ", dateOfBirth=" + dateOfBirth +
                ", startDate=" + startDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
