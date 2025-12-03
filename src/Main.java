import GUI.LoginDialog;
import GUI.MainGUI;
import jdbc.JdbcConnectionUtil;
import model.User;
import repository.EmployeeRepositoryImpl;
import repository.PersonRepositoryImpl;
import repository.RoleRepositoryImpl;
import repository.StudentRepositoryImpl;
import repository.UserRepositoryImpl;
import service.EmployeeService;
import service.EmployeeServiceImpl;
import service.PersonService;
import service.PersonServiceImpl;
import service.StudentService;
import service.StudentServiceImpl;
import service.UserService;
import service.UserServiceImpl;
import security.AuthManager;
import utils.LoggerUtil;
import utils.concurrent.AppExecutors;

import javax.swing.*;
import java.sql.Connection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Punkt startowy aplikacji. Inicjalizuje połączenie, repozytoria i serwisy,
 * dodatkowo tworzy admina jeśli brak.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		// 1) Inicjalizacja połączenia i podstawowych repo/serwisów (synchron)
		Connection conn = JdbcConnectionUtil.getConnection();

		// repozytoria aplikacyjne
		PersonRepositoryImpl personRepo = new PersonRepositoryImpl(conn);
		EmployeeRepositoryImpl employeeRepo = new EmployeeRepositoryImpl(conn);
		StudentRepositoryImpl studentRepo = new StudentRepositoryImpl(conn);

		// repozytoria auth
		UserRepositoryImpl userRepo = new UserRepositoryImpl(conn);
		RoleRepositoryImpl roleRepo = new RoleRepositoryImpl(conn);

		// serwisy
		PersonService personService = new PersonServiceImpl(personRepo);
		EmployeeService employeeService = new EmployeeServiceImpl(employeeRepo);
		StudentService studentService = new StudentServiceImpl(studentRepo);

		UserService userService = new UserServiceImpl(userRepo, roleRepo);

		// 2) Upewnij się że role istnieją i jest admin
		try {
		    roleRepo.findOrCreateRole("ROLE_USER");
		    roleRepo.findOrCreateRole("ROLE_ADMIN");

		    Optional<User> maybeAdmin = userService.findByUsername("admin").get();
		    if (maybeAdmin.isEmpty()) {
		        char[] defaultPass = "admin".toCharArray(); // production: nie rób tak
		        // użyj przeciążenia createUser z mustChange = true
		        User created = userService.createUser("admin", defaultPass, true, true).get();

		        // przypisz role
		        userService.assignRole("admin", "ROLE_ADMIN").get();
		        userService.assignRole("admin", "ROLE_USER").get();

		        LoggerUtil.log(Level.INFO, "Utworzono domyślnego admina 'admin' (hasło: admin) - wymuszono zmianę hasła przy pierwszym logowaniu!");
		    } else {
		        LoggerUtil.log(Level.INFO, "Admin już istnieje.");
		    }
		} catch (Exception ex) {
		    LoggerUtil.error("Błąd podczas inicjalizacji ról/użytkownika admin", ex);
		}

		// 3) Pokaż dialog logowania i (po powodzeniu) GUI - dialog jest modalny i
		// zwraca CompletableFuture
		LoginDialog loginDialog = new LoginDialog(null, userService);

		// showDialog() wyświetla modalny dialog (sam go zainicjuje na EDT). Tu
		// blokujemy thread main czekając na wynik.
		boolean loginOk;
		try {
			loginOk = loginDialog.showDialog().get();
		} catch (Exception ex) {
			LoggerUtil.error("Login dialog failed", ex);
			loginOk = false;
		}

		if (!loginOk) {
			LoggerUtil.log(Level.INFO, "Użytkownik zrezygnował z logowania. Kończę.");
			System.exit(0);
			return;
		}

		// 4) Po udanym logowaniu uruchamiamy główne GUI na EDT
		SwingUtilities.invokeLater(() -> {
			try {
				MainGUI gui = new MainGUI(personService, employeeService, studentService);
				// opcjonalnie pokaż kto jest zalogowany w tytule
				AuthManager.get().getCurrentUser()
						.ifPresent(u -> gui.setTitle(gui.getTitle() + " — zalogowany: " + u.getUsername()));
				gui.setVisible(true);
			} catch (Exception e) {
				LoggerUtil.error("Failed to start GUI", e);
				System.exit(1);
			}
		});
	}
}
