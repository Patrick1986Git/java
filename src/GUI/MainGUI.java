package GUI;

import model.Employee;
import model.Person;
import model.Student;
import service.EmployeeService;
import service.PersonService;
import service.StudentService;
import utils.CsvUtil;
import utils.LocalizationManager;
import utils.LoggerUtil;
import utils.concurrent.AppExecutors;
import security.AuthManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Główne okno aplikacji. Obsługuje asynchroniczne operacje (CompletableFuture).
 * - formularze zwracają CompletableFuture poprzez kontrolery, - po zakończeniu
 * operacji tabela jest automatycznie odświeżana.
 */
public class MainGUI extends JFrame {

	private final PersonService personService;
	private final EmployeeService employeeService;
	private final StudentService studentService;

	private final JTable table = new JTable();
	private final DefaultTableModel tableModel = new DefaultTableModel();

	private int page = 0;
	private int size = 20;
	private String currentEntity = "PERSON"; // PERSON / EMPLOYEE / STUDENT
	private String sortBy = "id";
	private boolean asc = true;

	// komponenty które chcemy kontrolować zależnie od roli
	private JLabel lblCurrentUser;
	private JButton btnAdd;
	private JButton btnEdit;
	private JButton btnDelete;

	public MainGUI(PersonService personService, EmployeeService employeeService, StudentService studentService) {
		super("Enterprise MVC App");
		this.personService = personService;
		this.employeeService = employeeService;
		this.studentService = studentService;

		initUI();
		loadData(); // pierwsze załadowanie
	}

	private void initUI() {
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(1000, 600);
		setLocationRelativeTo(null);

		JPanel top = new JPanel();
		JButton btnPrev = new JButton(LocalizationManager.getString("main.btn.prev"));
		JButton btnNext = new JButton(LocalizationManager.getString("main.btn.next"));
		JComboBox<String> cbEntity = new JComboBox<>(new String[] { "PERSON", "EMPLOYEE", "STUDENT" });
		btnAdd = new JButton(LocalizationManager.getString("main.btn.add"));
		btnEdit = new JButton(LocalizationManager.getString("main.btn.edit"));
		btnDelete = new JButton(LocalizationManager.getString("main.btn.delete"));
		JButton btnExport = new JButton(LocalizationManager.getString("main.btn.export"));

		top.add(new JLabel(LocalizationManager.getString("main.entity.label")));
		top.add(cbEntity);
		top.add(btnPrev);
		top.add(btnNext);
		top.add(btnAdd);
		top.add(btnEdit);
		top.add(btnDelete);
		top.add(btnExport);

		// language selector
		top.add(Box.createHorizontalStrut(16));
		top.add(new JLabel(LocalizationManager.getString("combo.lang.label")));
		JComboBox<Locale> langCombo = new JComboBox<>(new Locale[] { new Locale("pl"), Locale.ENGLISH });
		// renderer to show user-friendly names
		langCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof Locale) {
					Locale loc = (Locale) value;
					if ("pl".equals(loc.getLanguage()))
						setText(LocalizationManager.getString("lang.pl"));
					else if ("en".equals(loc.getLanguage()))
						setText(LocalizationManager.getString("lang.en"));
					else
						setText(loc.getDisplayName(loc));
				}
				return this;
			}
		});
		langCombo.setSelectedItem(LocalizationManager.getLocale());
		langCombo.addActionListener(e -> {
			Locale selected = (Locale) langCombo.getSelectedItem();
			if (selected != null) {
				LocalizationManager.setLocale(selected);
			}
		});
		top.add(langCombo);

		// current user label
		lblCurrentUser = new JLabel();
		AuthManager.get().getCurrentUser().ifPresent(
				u -> lblCurrentUser.setText(LocalizationManager.getString("main.lbl.user", u.getUsername())));
		top.add(Box.createHorizontalStrut(16));
		top.add(lblCurrentUser);

		getContentPane().add(top, BorderLayout.NORTH);

		// listeners
		cbEntity.addActionListener(e -> {
			currentEntity = (String) cbEntity.getSelectedItem();
			page = 0;
			loadData();
		});
		btnPrev.addActionListener(e -> {
			if (page > 0)
				page--;
			loadData();
		});
		btnNext.addActionListener(e -> {
			page++;
			loadData();
		});
		btnAdd.addActionListener(e -> onAdd());
		btnEdit.addActionListener(e -> onEdit());
		btnDelete.addActionListener(e -> onDelete());
		btnExport.addActionListener(e -> onExport());

		table.setModel(tableModel);
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		
		table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int col = table.columnAtPoint(e.getPoint());
				String name = table.getColumnName(col);
				sortBy = name.toLowerCase();
				asc = !asc;
				loadData();
			}
		});

		LocalizationManager.addListener(newLocale -> {
			// update static strings on EDT
			SwingUtilities.invokeLater(() -> {
				// update top buttons/labels
				btnAdd.setText(LocalizationManager.getString("main.btn.add"));
				btnEdit.setText(LocalizationManager.getString("main.btn.edit"));
				btnDelete.setText(LocalizationManager.getString("main.btn.delete"));
				btnExport.setText(LocalizationManager.getString("main.btn.export"));
				// update label for current user (if present)
				AuthManager.get().getCurrentUser().ifPresent(
						u -> lblCurrentUser.setText(LocalizationManager.getString("main.lbl.user", u.getUsername())));
				// update other UI pieces (table headers)
				// you can call loadData() to refresh column names (see below)
			});
		});

		// ustawienia zależne od roli
		boolean isAdmin = AuthManager.get().hasRole("ROLE_ADMIN");
		// prosty przykład: tylko admin może usuwać
		btnDelete.setEnabled(isAdmin);
		// pozostali przyciski dostępne dla wszystkich (dostosuj wedle potrzeb)

		// Odświeżanie tabeli co 60 sek.
		AppExecutors.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::loadData), 60, 60,
				java.util.concurrent.TimeUnit.SECONDS);
	}

	/**
	 * Ładuje dane odpowiedniej encji (asynchronicznie).
	 */
	private void loadData() {
		switch (currentEntity) {
		case "PERSON" -> personService.findAll(page, size, sortBy, asc).thenAccept(list -> {
			LoggerUtil.info("Loaded " + list.size() + " persons from DB (page=" + page + ")");
			SwingUtilities.invokeLater(() -> updateTableForPersons(list));
		}).exceptionally(ex -> handleLoadError("person", ex));

		case "EMPLOYEE" -> employeeService.findAll(page, size, sortBy, asc).thenAccept(list -> {
			LoggerUtil.info("Loaded " + list.size() + " employees from DB (page=" + page + ")");
			SwingUtilities.invokeLater(() -> updateTableForEmployees(list));
		}).exceptionally(ex -> handleLoadError("employee", ex));

		case "STUDENT" -> studentService.findAll(page, size, sortBy, asc).thenAccept(list -> {
			LoggerUtil.info("Loaded " + list.size() + " students from DB (page=" + page + ")");
			SwingUtilities.invokeLater(() -> updateTableForStudents(list));
		}).exceptionally(ex -> handleLoadError("student", ex));

		default -> LoggerUtil.warn("Nieznany typ encji: " + currentEntity);
		}

	}

	private Void handleLoadError(String what, Throwable ex) {
		LoggerUtil.error("Failed to load " + what, ex);
		SwingUtilities.invokeLater(
				() -> JOptionPane.showMessageDialog(this, "Błąd ładowania " + what + ": " + ex.getMessage()));
		return null;
	}

	// ===== update table methods (UI on EDT) =====
	private void updateTableForPersons(List<Person> list) {
		String[] cols = new String[] { "id", LocalizationManager.getString("person.field.name"),
				LocalizationManager.getString("person.field.surname"),
				LocalizationManager.getString("person.field.age"), LocalizationManager.getString("person.field.dob"),
				LocalizationManager.getString("person.field.start"), };
		tableModel.setDataVector(toTableDataForPersons(list), cols);
	}

	private void updateTableForEmployees(List<Employee> list) {
		String[] cols = new String[] { "id", LocalizationManager.getString("person.field.name"),
				LocalizationManager.getString("person.field.surname"),
				LocalizationManager.getString("person.field.age"), LocalizationManager.getString("person.field.dob"),
				LocalizationManager.getString("person.field.start"),
				LocalizationManager.getString("employee.field.salary"),
				LocalizationManager.getString("employee.field.position") };
		tableModel.setDataVector(toTableDataForEmployees(list), cols);
	}

	private void updateTableForStudents(List<Student> list) {
		String[] cols = new String[] { "id", LocalizationManager.getString("person.field.name"),
				LocalizationManager.getString("person.field.surname"),
				LocalizationManager.getString("person.field.age"), LocalizationManager.getString("person.field.dob"),
				LocalizationManager.getString("person.field.start"),
				LocalizationManager.getString("student.field.university"),
				LocalizationManager.getString("student.field.year") };
		tableModel.setDataVector(toTableDataForStudents(list), cols);
	}

	// ===== converters =====
	private Object[][] toTableDataForPersons(List<Person> list) {
		Object[][] data = new Object[list.size()][6];
		for (int i = 0; i < list.size(); i++) {
			Person p = list.get(i);
			data[i][0] = p.getId();
			data[i][1] = p.getName();
			data[i][2] = p.getSurname();
			data[i][3] = p.getAge();
			data[i][4] = formatDate(p.getDateOfBirth());
			data[i][5] = formatDate(p.getStartDate());
		}
		return data;
	}

	private Object[][] toTableDataForEmployees(List<Employee> list) {
		Object[][] data = new Object[list.size()][8];
		for (int i = 0; i < list.size(); i++) {
			Employee e = list.get(i);
			data[i][0] = e.getId();
			data[i][1] = e.getName();
			data[i][2] = e.getSurname();
			data[i][3] = e.getAge();
			data[i][4] = formatDate(e.getDateOfBirth());
			data[i][5] = formatDate(e.getStartDate());
			data[i][6] = e.getSalary();
			data[i][7] = e.getPosition();
		}
		return data;
	}

	private Object[][] toTableDataForStudents(List<Student> list) {
		Object[][] data = new Object[list.size()][8];
		for (int i = 0; i < list.size(); i++) {
			Student s = list.get(i);
			data[i][0] = s.getId();
			data[i][1] = s.getName();
			data[i][2] = s.getSurname();
			data[i][3] = s.getAge();
			data[i][4] = formatDate(s.getDateOfBirth());
			data[i][5] = formatDate(s.getStartDate());
			data[i][6] = s.getUniversity();
			data[i][7] = s.getYear();
		}
		return data;
	}

	private String formatDate(LocalDate date) {
		return date == null ? "" : date.toString();
	}

	// ===== CRUD flows: show form -> call service -> refresh table =====

	private void onAdd() {
		switch (currentEntity) {
		case "PERSON" -> {
			PersonFormController controller = new PersonFormController(this);
			controller.showForm(new Person(null, "", "", 18, null, null) {
			}).thenCompose(entity -> {
				if (entity == null)
					return CompletableFuture.completedFuture(null);
				// wywołaj service.create (zakładamy, że zwraca CompletableFuture<Person>)
				return personService.create(entity);
			}).thenRun(() -> loadData()) // odśwież po stworzeniu
					.exceptionally(ex -> {
						LoggerUtil.error("Create person failed", ex);
						SwingUtilities.invokeLater(
								() -> JOptionPane.showMessageDialog(this, "Błąd zapisu: " + ex.getMessage()));
						return null;
					});
		}
		case "EMPLOYEE" -> {
			EmployeeFormController controller = new EmployeeFormController(this);
			controller.showForm(new Employee(null, "", "", 25, null, null, 0.0, "")).thenCompose(entity -> {
				if (entity == null)
					return CompletableFuture.completedFuture(null);
				return employeeService.create(entity);
			}).thenRun(this::loadData).exceptionally(ex -> {
				LoggerUtil.error("Create employee failed", ex);
				SwingUtilities
						.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd zapisu: " + ex.getMessage()));
				return null;
			});
		}
		case "STUDENT" -> {
			StudentFormController controller = new StudentFormController(this);
			controller.showForm(new Student(null, "", "", 20, null, null, "", 1)).thenCompose(entity -> {
				if (entity == null)
					return CompletableFuture.completedFuture(null);
				return studentService.create(entity);
			}).thenRun(this::loadData).exceptionally(ex -> {
				LoggerUtil.error("Create student failed", ex);
				SwingUtilities
						.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd zapisu: " + ex.getMessage()));
				return null;
			});
		}
		}
	}

	private void onEdit() {
		int row = table.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, LocalizationManager.getString("ui.select.row"),
					LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		Integer id = (Integer) table.getValueAt(row, 0);

		switch (currentEntity) {
		case "PERSON" -> personService.findById(id).thenCompose(opt -> {
			if (opt.isEmpty()) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Nie znaleziono"));
				return CompletableFuture.completedFuture(null);
			}
			Person p = opt.get();
			PersonFormController ctrl = new PersonFormController(this);
			return ctrl.showForm(p).thenCompose(updated -> {
				if (updated == null)
					return CompletableFuture.completedFuture(null);
				return personService.update(updated);
			});
		}).thenRun(this::loadData).exceptionally(ex -> {
			LoggerUtil.error("Edit person failed", ex);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd: " + ex.getMessage()));
			return null;
		});

		case "EMPLOYEE" -> employeeService.findById(id).thenCompose(opt -> {
			if (opt.isEmpty()) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Nie znaleziono"));
				return CompletableFuture.completedFuture(null);
			}
			Employee e = opt.get();
			EmployeeFormController ctrl = new EmployeeFormController(this);
			return ctrl.showForm(e).thenCompose(updated -> {
				if (updated == null)
					return CompletableFuture.completedFuture(null);
				return employeeService.update(updated);
			});
		}).thenRun(this::loadData).exceptionally(ex -> {
			LoggerUtil.error("Edit employee failed", ex);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd: " + ex.getMessage()));
			return null;
		});

		case "STUDENT" -> studentService.findById(id).thenCompose(opt -> {
			if (opt.isEmpty()) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Nie znaleziono"));
				return CompletableFuture.completedFuture(null);
			}
			Student s = opt.get();
			StudentFormController ctrl = new StudentFormController(this);
			return ctrl.showForm(s).thenCompose(updated -> {
				if (updated == null)
					return CompletableFuture.completedFuture(null);
				return studentService.update(updated);
			});
		}).thenRun(this::loadData).exceptionally(ex -> {
			LoggerUtil.error("Edit student failed", ex);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd: " + ex.getMessage()));
			return null;
		});
		}
	}

	private void onDelete() {
		int row = table.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Wybierz wiersz");
			return;
		}
		Integer id = (Integer) table.getValueAt(row, 0);
		int confirm = JOptionPane.showConfirmDialog(this, LocalizationManager.getString("ui.confirm.delete"),
				LocalizationManager.getString("dialog.confirm.title"), JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION)
			return;

		CompletableFuture<Void> deletionTask = switch (currentEntity) {
		case "PERSON" -> personService.deleteById(id).thenAccept(ok -> {
		});
		case "EMPLOYEE" -> employeeService.deleteById(id).thenAccept(ok -> {
		});
		default -> studentService.deleteById(id).thenAccept(ok -> {
		});
		};

		deletionTask.thenRun(this::loadData).exceptionally(ex -> {
			LoggerUtil.error("Delete failed", ex);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd usuwania: " + ex.getMessage()));
			return null;
		});
	}

	private void onExport() {
		JFileChooser fc = new JFileChooser();
		int ret = fc.showSaveDialog(this);
		if (ret != JFileChooser.APPROVE_OPTION)
			return;
		File f = fc.getSelectedFile();

		// Pobierz snapshot danych z modelu tabeli i zapisz asynchronicznie w
		// IO_EXECUTOR
		List<String[]> rows = new ArrayList<>();
		for (int r = 0; r < tableModel.getRowCount(); r++) {
			String[] row = new String[tableModel.getColumnCount()];
			for (int c = 0; c < tableModel.getColumnCount(); c++) {
				Object val = tableModel.getValueAt(r, c);
				row[c] = val == null ? "" : val.toString();
			}
			rows.add(row);
		}

		CompletableFuture.runAsync(() -> {
			try {
				CsvUtil.writeCsv(f.getAbsolutePath(), rows);
				LoggerUtil.info("Wyeksportowano CSV: " + f.getAbsolutePath());
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("ui.export.success", f.getAbsolutePath()),
						LocalizationManager.getString("dialog.info.title"), JOptionPane.INFORMATION_MESSAGE));
			} catch (Exception ex) {
				LoggerUtil.error("Export failed", ex);
				SwingUtilities
						.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd eksportu: " + ex.getMessage()));
			}
		}, AppExecutors.IO_EXECUTOR);
	}
}
