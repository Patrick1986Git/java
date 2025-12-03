package model;

import java.time.LocalDate;

/**
 * Pracownik - rozszerza Person o pola salary i position.
 */
public class Employee extends Person {
	private Double salary;
	private String position;

	public Employee() {
		super();
	}

	public Employee(Integer id, String name, String surname, Integer age, LocalDate dateOfBirth, LocalDate startDate,
			Double salary, String position) {
		super(id, name, surname, age, dateOfBirth, startDate);
		this.salary = salary == null ? 0.0 : salary;
		this.position = position == null ? "" : position.trim();
	}

	public Double getSalary() {
		return salary;
	}

	public void setSalary(Double salary) {
		this.salary = salary == null ? 0.0 : salary;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position == null ? "" : position.trim();
	}

	@Override
	public String toString() {
		return super.toString() + ", salary=" + salary + ", position='" + position + '\'' + '}';
	}
}
