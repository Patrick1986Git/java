package utils.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

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
			return ValidationResult.error("Wypełnij imię.");
		if (name.trim().length() > 100)
			return ValidationResult.error("Imię jest za długie.");
		return ValidationResult.ok();
	}

	public static ValidationResult validateSurname(String surname) {
		if (surname == null || surname.trim().isEmpty())
			return ValidationResult.error("Wypełnij nazwisko.");
		if (surname.trim().length() > 100)
			return ValidationResult.error("Nazwisko jest za długie.");
		return ValidationResult.ok();
	}

	public static ValidationResult validateAge(Integer age, int min, int max) {
		if (age == null)
			return ValidationResult.error("Podaj wiek.");
		if (age < min || age > max)
			return ValidationResult.error("Wiek musi być w zakresie " + min + "–" + max + " lat.");
		return ValidationResult.ok();
	}

	public static ValidationResult validateDateText(String dateText) {
		if (dateText == null || dateText.trim().isEmpty())
			return ValidationResult.error("Brak daty.");
		try {
			LocalDate.parse(dateText.trim(), DEFAULT_FORMATTER);
			return ValidationResult.ok();
		} catch (DateTimeParseException e) {
			return ValidationResult.error("Niepoprawny format daty. Użyj yyyy-MM-dd.");
		}
	}

	public static ValidationResult validateDatesLogic(LocalDate dob, LocalDate startDate) {
		if (dob != null && startDate != null && startDate.isBefore(dob)) {
			return ValidationResult.error("Data rozpoczęcia nie może być wcześniejsza niż data urodzenia.");
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
		// dodatkowe reguły (np. start nie dalej niż +1 rok) możesz dodać tu
		if (start != null && start.isAfter(LocalDate.now().plusYears(1)))
			return ValidationResult.error("Data rozpoczęcia jest zbyt odległa w przyszłości.");
		if (dob != null && dob.isAfter(LocalDate.now()))
			return ValidationResult.error("Data urodzenia nie może być w przyszłości.");
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
			return ValidationResult.error("Wynagrodzenie musi być większe od 0!");
		}

		if (position == null || position.trim().isEmpty()) {
			return ValidationResult.error("Wypełnij stanowisko!");
		}

		if (position.length() > 100) {
			return ValidationResult.error("Nazwa stanowiska jest za długa!");
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
			return ValidationResult.error("Wypełnij nazwę uczelni!");
		}

		if (university.length() > 150) {
			return ValidationResult.error("Nazwa uczelni jest za długa!");
		}

		if (year == null || year < 1 || year > 10) {
			return ValidationResult.error("Rok studiów musi być w zakresie 1–10!");
		}

		// dodatkowa logika: start nie może być wcześniej niż 15 lat po urodzeniu
		LocalDate dob = parseDateOrNull(dobText);
		LocalDate start = parseDateOrNull(startText);
		if (dob != null && start != null && start.isBefore(dob.plusYears(15))) {
			return ValidationResult
					.error("Data rozpoczęcia studiów nie może być wcześniejsza niż 15 lat po dacie urodzenia!");
		}

		return ValidationResult.ok();
	}

}
