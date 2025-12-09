package utils.validation;

import utils.LocalizationManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Prosty helper walidacyjny dla formularzy (name, surname, age, dates, etc).
 * Zwraca ValidationResult (ok() lub error(msg)). Możesz rozszerzyć o metody
 * specyficzne dla Employee/Student.
 */
public final class FormValidators {

	private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private FormValidators() {
	}

	public static ValidationResult validateName(String name) {
		if (name == null || name.trim().isEmpty())
			return ValidationResult.error(LocalizationManager.getString("validation.name.required"));
		if (name.trim().length() > 100)
			return ValidationResult.error(LocalizationManager.getString("validation.name.toolong"));
		return ValidationResult.ok();
	}

	public static ValidationResult validateSurname(String surname) {
		if (surname == null || surname.trim().isEmpty())
			return ValidationResult.error(LocalizationManager.getString("validation.surname.required"));
		if (surname.trim().length() > 100)
			return ValidationResult.error(LocalizationManager.getString("validation.surname.toolong"));
		return ValidationResult.ok();
	}

	public static ValidationResult validateAge(Integer age, int min, int max) {
		if (age == null)
			return ValidationResult.error(LocalizationManager.getString("validation.age.required"));
		if (age < min || age > max)
			return ValidationResult.error(LocalizationManager.getString("validation.age.range", min, max));
		return ValidationResult.ok();
	}

	public static ValidationResult validateDateText(String dateText) {
		if (dateText == null || dateText.trim().isEmpty())
			return ValidationResult.error(LocalizationManager.getString("validation.date.missing"));
		try {
			LocalDate.parse(dateText.trim(), DEFAULT_FORMATTER);
			return ValidationResult.ok();
		} catch (DateTimeParseException e) {
			return ValidationResult.error(LocalizationManager.getString("validation.date.invalid"));
		}
	}

	public static ValidationResult validateDatesLogic(LocalDate dob, LocalDate startDate) {
		if (dob != null && startDate != null && startDate.isBefore(dob)) {
			return ValidationResult.error(LocalizationManager.getString("validation.dates.logic"));
		}
		return ValidationResult.ok();
	}

	// Parsowanie z bezpiecznym zwrotem null (użyj po validateDateText jeśli
	// wymagane)
	public static LocalDate parseDateOrNull(String text) {
		if (text == null || text.trim().isEmpty())
			return null;
		try {
			return LocalDate.parse(text.trim(), DEFAULT_FORMATTER);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	// Przykładowy komplet validatorów dla Person (możesz rozdzielić dla
	// Student/Employee)
	public static ValidationResult validatePersonInputs(String name, String surname, Integer age, String dobText,
			String startText) {
		ValidationResult r;
		r = validateName(name);
		if (!r.isValid())
			return r;
		r = validateSurname(surname);
		if (!r.isValid())
			return r;
		r = validateAge(age, 0, 120);
		if (!r.isValid())
			return r;
		r = validateDateText(dobText);
		if (!r.isValid())
			return r;
		r = validateDateText(startText);
		if (!r.isValid())
			return r;
		LocalDate dob = parseDateOrNull(dobText);
		LocalDate start = parseDateOrNull(startText);
		r = validateDatesLogic(dob, start);
		if (!r.isValid())
			return r;
		// dodatkowe reguły (np. start nie dalej niż +1 rok)
		if (start != null && start.isAfter(LocalDate.now().plusYears(1)))
			return ValidationResult.error(LocalizationManager.getString("validation.start.too_far"));
		if (dob != null && dob.isAfter(LocalDate.now()))
			return ValidationResult.error(LocalizationManager.getString("validation.dob.future"));
		return ValidationResult.ok();
	}

	// --- Walidacja formularza Employee ---
	public static ValidationResult validateEmployeeInputs(String name, String surname, Integer age, String dobText,
			String startText, Double salary, String position) {
		// Walidacja wspólnych pól
		ValidationResult r = validatePersonInputs(name, surname, age, dobText, startText);
		if (!r.isValid())
			return r;

		// Walidacja specyficzna
		if (salary == null || salary <= 0) {
			return ValidationResult.error(LocalizationManager.getString("validation.employee.salary.positive"));
		}

		if (position == null || position.trim().isEmpty()) {
			return ValidationResult.error(LocalizationManager.getString("validation.employee.position.required"));
		}

		if (position.length() > 100) {
			return ValidationResult.error(LocalizationManager.getString("validation.employee.position.toolong"));
		}

		return ValidationResult.ok();
	}

	// --- Walidacja formularza Student ---
	public static ValidationResult validateStudentInputs(String name, String surname, Integer age, String dobText,
			String startText, String university, Integer year) {
		// Walidacja wspólnych pól
		ValidationResult r = validatePersonInputs(name, surname, age, dobText, startText);
		if (!r.isValid())
			return r;

		// Walidacja specyficzna
		if (university == null || university.trim().isEmpty()) {
			return ValidationResult.error(LocalizationManager.getString("validation.student.university.required"));
		}

		if (university.length() > 150) {
			return ValidationResult.error(LocalizationManager.getString("validation.student.university.toolong"));
		}

		if (year == null || year < 1 || year > 10) {
			return ValidationResult.error(LocalizationManager.getString("validation.student.year.range"));
		}

		// dodatkowa logika: start nie może być wcześniej niż 15 lat po urodzeniu
		LocalDate dob = parseDateOrNull(dobText);
		LocalDate start = parseDateOrNull(startText);
		if (dob != null && start != null && start.isBefore(dob.plusYears(15))) {
			return ValidationResult.error(LocalizationManager.getString("validation.student.start.tooyoung"));
		}

		return ValidationResult.ok();
	}

}
