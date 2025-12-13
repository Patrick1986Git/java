package dto.statistics;

import java.util.Map;

public class AgeDistributionDTO {

	private Map<Integer, Long> persons;
	private Map<Integer, Long> employees;
	private Map<Integer, Long> students;

	public AgeDistributionDTO(Map<Integer, Long> persons, Map<Integer, Long> employees, Map<Integer, Long> students) {
		this.persons = persons;
		this.employees = employees;
		this.students = students;
	}

	public Map<Integer, Long> getPersons() {
		return persons;
	}

	public Map<Integer, Long> getEmployees() {
		return employees;
	}

	public Map<Integer, Long> getStudents() {
		return students;
	}
}