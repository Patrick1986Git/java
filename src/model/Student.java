package model;

import java.time.LocalDate;

/**
 * Student - rozszerza Person o pola university i year.
 */
public class Student extends Person {
	private String university;
	private Integer year;

	public Student() {
		super();
	}

	public Student(Integer id, String name, String surname, Integer age, LocalDate dateOfBirth, LocalDate startDate,
			String university, Integer year) {
		super(id, name, surname, age, dateOfBirth, startDate);
		this.university = university == null ? "" : university.trim();
		this.year = year == null ? 1 : year;
	}

	public String getUniversity() {
		return university;
	}

	public void setUniversity(String university) {
		this.university = university == null ? "" : university.trim();
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year == null ? 1 : year;
	}

	@Override
	public String toString() {
		return super.toString() + ", university='" + university + '\'' + ", year=" + year + '}';
	}
}
