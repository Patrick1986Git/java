package service;

import model.Person;
import model.Employee;
import model.Student;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import dto.statistics.AgeDistributionDTO;

public class StatisticsService {

	private final PersonService personService;
	private final EmployeeService employeeService;
	private final StudentService studentService;

	public StatisticsService(PersonService personService, EmployeeService employeeService,
			StudentService studentService) {
		this.personService = personService;
		this.employeeService = employeeService;
		this.studentService = studentService;
	}

	private Map<Integer, Long> groupByAge(List<Integer> ages) {
		return ages.stream().filter(a -> a != null && a > 0 && a < 150)
				.collect(Collectors.groupingBy(a -> a, Collectors.counting()));
	}

	private List<Integer> extractAgesFromPeople(List<? extends Object> list) {
		return list.stream().map(obj -> {
			try {
				Object v = obj.getClass().getMethod("getAge").invoke(obj);
				return v == null ? null : (Integer) v;
			} catch (Exception e) {
				return null;
			}
		}).collect(Collectors.toList());
	}

	/**
	 * Asynchroniczne ładowanie i budowanie DTO
	 */
	public CompletableFuture<AgeDistributionDTO> loadAgeDistributionAsync() {
		CompletableFuture<List<Person>> fPersons = personService.findAll();
		CompletableFuture<List<Employee>> fEmployees = employeeService.findAll();
		CompletableFuture<List<Student>> fStudents = studentService.findAll();

		return CompletableFuture.allOf(fPersons, fEmployees, fStudents).thenApply(v -> {
			List<Person> persons = fPersons.join();
			List<Employee> employees = fEmployees.join();
			List<Student> students = fStudents.join();

			Map<Integer, Long> per = groupByAge(extractAgesFromPeople(persons));
			Map<Integer, Long> emp = groupByAge(extractAgesFromPeople(employees));
			Map<Integer, Long> stu = groupByAge(extractAgesFromPeople(students));

			return new AgeDistributionDTO(per, emp, stu);
		});
	}
}
