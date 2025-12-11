package GUI;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Główne okno aplikacji.
 */
public class MainGUI extends JFrame {

	private final PersonService personService;
	private final EmployeeService employeeService;
	private final StudentService studentService;

	private final JTable table;
	private final DefaultTableModel tableModel;

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

	// pola dostępne w całej klasie (używane w listenerach)
	private JButton btnPrev;
	private JButton btnNext;
	private JButton btnExport;
	private JComboBox<String> cbEntity;
	private JComboBox<Locale> langCombo;
	private JLabel lblEntity;
	private JLabel lblLang;

	// store base preferred sizes for top buttons so normalizeTopButtonWidths won't
	// grow sizes repeatedly
	private final Map<JButton, Dimension> baseButtonSizes = new HashMap<>();

	public MainGUI(PersonService personService, EmployeeService employeeService, StudentService studentService) {
		super();
		this.personService = personService;
		this.employeeService = employeeService;
		this.studentService = studentService;

		tableModel = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // tabela tylko do odczytu
			}
		};
		table = new JTable(tableModel);

		// Init UI on EDT (constructor may run on EDT; it's fine)
		initUI();

		// ensure title and initial data
		updateWindowTitle();
		loadData();
		captureBaseButtonSizes();
		computeAndApplyLabelWidths();
		normalizeTopButtonWidths();
	}

	private void initUI() {
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(1700, 600);
		setLocationRelativeTo(null);

		// Use default panel background from L&F for content pane so area outside table
		// looks standard
		Color panelBg = UIManager.getColor("Panel.background");
		if (panelBg != null) {
			getContentPane().setBackground(panelBg);
		}

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		top.setOpaque(true);
		if (panelBg != null)
			top.setBackground(panelBg);

		// entity label + combobox
		lblEntity = new JLabel(LocalizationManager.getString("main.entity.label"));
		top.add(lblEntity);

		DefaultComboBoxModel<String> entityModel = new DefaultComboBoxModel<>(
				new String[] { "PERSON", "EMPLOYEE", "STUDENT" });
		cbEntity = new JComboBox<>(entityModel);
		cbEntity.setSelectedItem(currentEntity);
		cbEntity.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof String) {
					String code = (String) value;
					switch (code) {
					case "PERSON" -> setText(LocalizationManager.getString("entity.person"));
					case "EMPLOYEE" -> setText(LocalizationManager.getString("entity.employee"));
					case "STUDENT" -> setText(LocalizationManager.getString("entity.student"));
					default -> setText(code);
					}
				}
				return this;
			}
		});
		top.add(cbEntity);

		// paging buttons
		btnPrev = new JButton(LocalizationManager.getString("main.btn.prev"));
		btnNext = new JButton(LocalizationManager.getString("main.btn.next"));
		top.add(btnPrev);
		top.add(btnNext);

		// action buttons
		btnAdd = new JButton(LocalizationManager.getString("main.btn.add"));
		btnEdit = new JButton(LocalizationManager.getString("main.btn.edit"));
		btnDelete = new JButton(LocalizationManager.getString("main.btn.delete"));
		btnExport = new JButton(LocalizationManager.getString("main.btn.export"));

		top.add(btnAdd);
		top.add(btnEdit);
		top.add(btnDelete);
		top.add(btnExport);

		// language selector
		top.add(Box.createHorizontalStrut(16));
		lblLang = new JLabel(LocalizationManager.getString("combo.lang.label"));
		top.add(lblLang);

		langCombo = new JComboBox<>(new Locale[] { new Locale("pl"), Locale.ENGLISH });
		langCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
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
		top.add(langCombo);

		// current user label
		lblCurrentUser = new JLabel();
		AuthManager.get().getCurrentUser().ifPresent(
				u -> lblCurrentUser.setText(LocalizationManager.getString("main.lbl.user", u.getUsername())));
		top.add(Box.createHorizontalStrut(16));
		top.add(lblCurrentUser);

		getContentPane().add(top, BorderLayout.NORTH);

		// table + scroll:
		JScrollPane scroll = new JScrollPane(table);

		// --- IMPORTANT fixes for desired look ---
		// 1) Make viewport (area behind table) use panel background (so area below rows
		// is grey)
		Color spBg = panelBg != null ? panelBg : getContentPane().getBackground();
		scroll.getViewport().setBackground(spBg); // <-- viewport background = panel color
		scroll.setBackground(spBg); // scroll background same as panel
		// 2) Keep cells area white by using cell renderer / table background but
		// do NOT force table to fill the viewport height -> empty area will show
		// viewport bg.
		table.setBackground(Color.WHITE);
		table.setOpaque(true);
		table.setFillsViewportHeight(false); // <-- do NOT fill viewport

		table.setGridColor(Color.LIGHT_GRAY);
		table.getTableHeader().setReorderingAllowed(false);
		table.setFillsViewportHeight(false); // ensure we don't force fill

		getContentPane().add(scroll, BorderLayout.CENTER);

		// Listeners / actions
		cbEntity.addActionListener(e -> {
			currentEntity = (String) cbEntity.getSelectedItem();
			page = 0;
			loadData();
		});

		btnPrev.addActionListener(e -> {
			if (page > 0) {
				page--;
				loadData();
			}
		});
		btnNext.addActionListener(e -> {
			page++;
			loadData();
		});

		btnAdd.addActionListener(e -> onAdd());
		btnEdit.addActionListener(e -> onEdit());
		btnDelete.addActionListener(e -> onDelete());
		btnExport.addActionListener(e -> onExport());

		// double click row opens edit
		table.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2) {
					onEdit();
				}
			}
		});

		// sort by header click
		table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int col = table.columnAtPoint(e.getPoint());
				if (col >= 0) {
					String name = table.getColumnName(col);
					sortBy = name.toLowerCase();
					asc = !asc;
					loadData();
				}
			}
		});

		// When language changes update texts (and normalize sizes)
		LocalizationManager.addListener(newLocale -> SwingUtilities.invokeLater(() -> {
			btnPrev.setText(LocalizationManager.getString("main.btn.prev"));
			btnNext.setText(LocalizationManager.getString("main.btn.next"));
			btnAdd.setText(LocalizationManager.getString("main.btn.add"));
			btnEdit.setText(LocalizationManager.getString("main.btn.edit"));
			btnDelete.setText(LocalizationManager.getString("main.btn.delete"));
			btnExport.setText(LocalizationManager.getString("main.btn.export"));
			lblEntity.setText(LocalizationManager.getString("main.entity.label"));
			lblLang.setText(LocalizationManager.getString("combo.lang.label"));
			AuthManager.get().getCurrentUser().ifPresentOrElse(
					u -> lblCurrentUser.setText(LocalizationManager.getString("main.lbl.user", u.getUsername())),
					() -> lblCurrentUser.setText(""));
			// update combos renderers
			langCombo.repaint();
			cbEntity.repaint();
			// re-apply fixed widths computed earlier (we keep stable sizes)
			normalizeTopButtonWidths();
			// reload data (so column headers localized)
			page = Math.max(0, page);
			loadData();
			updateWindowTitle();
		}));

		// language selection action
		langCombo.addActionListener(e -> {
			Locale selected = (Locale) langCombo.getSelectedItem();
			if (selected != null) {
				LocalizationManager.setLocale(selected);
			}
		});

		// role based controls
		boolean isAdmin = AuthManager.get().hasRole("ROLE_ADMIN");
		btnDelete.setEnabled(isAdmin);

		// auto refresh
		AppExecutors.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::loadData), 60, 60,
				java.util.concurrent.TimeUnit.SECONDS);

	}

	/**
	 * Zmieniona logika: dla każdego przycisku zapisujemy jako "base" wymiar
	 * maksymalny biorąc pod uwagę tekst dla PL i EN (żeby nie skakały przy
	 * przełączeniu).
	 */
	private void captureBaseButtonSizes() {
	    JButton[] buttonsToStore = new JButton[] { btnPrev, btnNext, btnAdd, btnEdit, btnDelete, btnExport };
	    Locale[] checkLocales = new Locale[] { Locale.ENGLISH, new Locale("pl") };

	    // create off-screen graphics for reliable FontMetrics regardless of L&F initialization
	    java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
	    Graphics2D gx = bi.createGraphics();

	    for (JButton b : buttonsToStore) {
	        if (b == null)
	            continue;

	        Font font = b.getFont();
	        if (font == null) font = UIManager.getFont("Button.font");
	        gx.setFont(font);
	        FontMetrics fm = gx.getFontMetrics(font);

	        int maxW = 0;
	        int maxH = fm.getHeight();

	        // take into account current preferred size as baseline
	        Dimension pref = b.getPreferredSize();
	        if (pref != null) {
	            maxW = Math.max(maxW, pref.width);
	            maxH = Math.max(maxH, pref.height);
	        }

	        String key = inferKeyForButton(b);
	        if (key != null) {
	            for (Locale loc : checkLocales) {
	                String text = getStringForLocale(key, loc);
	                if (text == null) text = LocalizationManager.getString(key);
	                if (text != null) {
	                    int w = fm.stringWidth(text) + 24; // padding
	                    maxW = Math.max(maxW, w);
	                    maxH = Math.max(maxH, fm.getHeight() + 6);
	                }
	            }
	        } else {
	            // fallback: measure current button text
	            String text = b.getText();
	            if (text != null) {
	                maxW = Math.max(maxW, fm.stringWidth(text) + 24);
	            }
	        }

	        baseButtonSizes.put(b, new Dimension(maxW, maxH));
	    }

	    gx.dispose();
	}


	// helper: approximate mapping from JButton instance to localization key
	private String inferKeyForButton(JButton b) {
		if (b == btnPrev)
			return "main.btn.prev";
		if (b == btnNext)
			return "main.btn.next";
		if (b == btnAdd)
			return "main.btn.add";
		if (b == btnEdit)
			return "main.btn.edit";
		if (b == btnDelete)
			return "main.btn.delete";
		if (b == btnExport)
			return "main.btn.export";
		return null;
	}

	// small helper to load a single localized string for given locale using same
	// base as LocalizationManager
	private String getStringForLocale(String key, Locale locale) {
		try {
			// replicate the UTF-8 ResourceBundle control used in LocalizationManager
			ResourceBundle.Control utf8Control = new ResourceBundle.Control() {
				@Override
				public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
						boolean reload) throws IllegalAccessException, InstantiationException, java.io.IOException {
					String bundleName = toBundleName(baseName, locale);
					String resourceName = toResourceName(bundleName, "properties");
					try (java.io.InputStream stream = loader.getResourceAsStream(resourceName)) {
						if (stream == null)
							return null;
						try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream,
								java.nio.charset.StandardCharsets.UTF_8)) {
							return new java.util.PropertyResourceBundle(reader);
						}
					}
				}
			};
			ResourceBundle rb = ResourceBundle.getBundle("resources.i18n.messages", locale,
					LocalizationManager.class.getClassLoader(), utf8Control);
			return rb.getString(key);
		} catch (Exception ex) {
			return null;
		}
	}

	private void computeAndApplyLabelWidths() {
	    Font font = lblEntity.getFont();
	    if (font == null) font = UIManager.getFont("Label.font");

	    // off-screen graphics
	    java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
	    Graphics2D gx = bi.createGraphics();
	    gx.setFont(font);
	    FontMetrics fm = gx.getFontMetrics(font);

	    String[] keys = new String[] { "main.entity.label", "combo.lang.label" };
	    int maxW = 0;
	    Locale[] checkLocales = new Locale[] { Locale.ENGLISH, new Locale("pl") };
	    for (String k : keys) {
	        for (Locale loc : checkLocales) {
	            String txt = getStringForLocale(k, loc);
	            if (txt == null) txt = LocalizationManager.getString(k);
	            if (txt != null) {
	                int w = fm.stringWidth(txt);
	                maxW = Math.max(maxW, w);
	            }
	        }
	    }
	    gx.dispose();

	    // add padding so text never touches edge
	    maxW += 24;
	    Dimension dEntity = lblEntity.getPreferredSize();
	    dEntity = new Dimension(maxW, dEntity.height);
	    lblEntity.setPreferredSize(dEntity);
	    Dimension dLang = lblLang.getPreferredSize();
	    dLang = new Dimension(maxW, dLang.height);
	    lblLang.setPreferredSize(dLang);
	    revalidate();
	    repaint();
	}


	private void updateWindowTitle() {
		String title = LocalizationManager.getString("app.title");
		String userPart = AuthManager.get().getCurrentUser()
				.map(u -> LocalizationManager.getString("main.lbl.user", u.getUsername())).orElse("");
		if (!userPart.isEmpty())
			title = title + " — " + userPart;
		setTitle(title);
	}

	private void normalizeTopButtonWidths() {
		List<JButton> list = new ArrayList<>();
		if (btnPrev != null)
			list.add(btnPrev);
		if (btnNext != null)
			list.add(btnNext);
		if (btnAdd != null)
			list.add(btnAdd);
		if (btnEdit != null)
			list.add(btnEdit);
		if (btnDelete != null)
			list.add(btnDelete);
		if (btnExport != null)
			list.add(btnExport);

		int max = 0;
		for (JButton b : list) {
			Dimension base = baseButtonSizes.get(b);
			int width = (base != null) ? base.width : b.getPreferredSize().width;
			max = Math.max(max, width);
		}
		max += 6; // small padding
		for (JButton b : list) {
			Dimension base = baseButtonSizes.get(b);
			int height = (base != null) ? base.height : b.getPreferredSize().height;
			b.setPreferredSize(new Dimension(max, height));
		}
		revalidate();
		repaint();
	}

	/**
	 * Ładuje dane odpowiedniej encji (asynchronicznie) oraz aktualizuje paginację.
	 */
	private void loadData() {
		switch (currentEntity) {
		case "PERSON" -> {
			personService.count().thenAccept(total -> SwingUtilities.invokeLater(() -> updatePaginationControls(total)))
					.exceptionally(ex -> {
						LoggerUtil.error("Count persons failed", ex);
						return null;
					});

			personService.findAll(page, size, sortBy, asc).thenAccept(list -> {
				LoggerUtil.info("Loaded " + list.size() + " persons from DB (page=" + page + ")");
				SwingUtilities.invokeLater(() -> updateTableForPersons(list));
			}).exceptionally(ex -> handleLoadError("person", ex));
		}
		case "EMPLOYEE" -> {
			employeeService.count()
					.thenAccept(total -> SwingUtilities.invokeLater(() -> updatePaginationControls(total)))
					.exceptionally(ex -> {
						LoggerUtil.error("Count employees failed", ex);
						return null;
					});

			employeeService.findAll(page, size, sortBy, asc).thenAccept(list -> {
				LoggerUtil.info("Loaded " + list.size() + " employees from DB (page=" + page + ")");
				SwingUtilities.invokeLater(() -> updateTableForEmployees(list));
			}).exceptionally(ex -> handleLoadError("employee", ex));
		}
		case "STUDENT" -> {
			studentService.count()
					.thenAccept(total -> SwingUtilities.invokeLater(() -> updatePaginationControls(total)))
					.exceptionally(ex -> {
						LoggerUtil.error("Count students failed", ex);
						return null;
					});

			studentService.findAll(page, size, sortBy, asc).thenAccept(list -> {
				LoggerUtil.info("Loaded " + list.size() + " students from DB (page=" + page + ")");
				SwingUtilities.invokeLater(() -> updateTableForStudents(list));
			}).exceptionally(ex -> handleLoadError("student", ex));
		}
		default -> LoggerUtil.warn("Nieznany typ encji: " + currentEntity);
		}
		updateWindowTitle();
	}

	private void updatePaginationControls(long totalRecords) {
		int maxPage = Math.max(0, (int) ((totalRecords - 1) / size));
		if (page > maxPage)
			page = maxPage;
		btnPrev.setEnabled(page > 0);
		btnNext.setEnabled(page < maxPage);
	}

	private Void handleLoadError(String what, Throwable ex) {
		LoggerUtil.error("Failed to load " + what, ex);
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
				LocalizationManager.getString("ui.load.error", what) + ": " + ex.getMessage()));
		return null;
	}

	// ===== update table methods (UI on EDT) =====
	private void updateTableForPersons(List<Person> list) {
		String[] cols = new String[] { "id", LocalizationManager.getString("person.field.name"),
				LocalizationManager.getString("person.field.surname"),
				LocalizationManager.getString("person.field.age"), LocalizationManager.getString("person.field.dob"),
				LocalizationManager.getString("person.field.start") };
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

	// ===== CRUD flows =====

	private void onAdd() {
		switch (currentEntity) {
		case "PERSON" -> {
			PersonFormController controller = new PersonFormController(this);
			controller.showForm(new Person(null, "", "", 18, null, null) {
			}).thenCompose(entity -> {
				if (entity == null)
					return CompletableFuture.completedFuture(null);
				return personService.create(entity);
			}).thenRun(this::loadData).exceptionally(ex -> {
				LoggerUtil.error("Create person failed", ex);
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("ui.save.error", ex.getMessage())));
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
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("ui.save.error", ex.getMessage())));
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
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("ui.save.error", ex.getMessage())));
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
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(this, LocalizationManager.getString("ui.notfound")));
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
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					LocalizationManager.getString("ui.edit.error", ex.getMessage())));
			return null;
		});

		case "EMPLOYEE" -> employeeService.findById(id).thenCompose(opt -> {
			if (opt.isEmpty()) {
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(this, LocalizationManager.getString("ui.notfound")));
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
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					LocalizationManager.getString("ui.edit.error", ex.getMessage())));
			return null;
		});

		case "STUDENT" -> studentService.findById(id).thenCompose(opt -> {
			if (opt.isEmpty()) {
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(this, LocalizationManager.getString("ui.notfound")));
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
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					LocalizationManager.getString("ui.edit.error", ex.getMessage())));
			return null;
		});
		}
	}

	private void onDelete() {
		int row = table.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, LocalizationManager.getString("ui.select.row"));
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
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					LocalizationManager.getString("ui.delete.error", ex.getMessage())));
			return null;
		});
	}

	private void onExport() {
		JFileChooser fc = new JFileChooser();
		int ret = fc.showSaveDialog(this);
		if (ret != JFileChooser.APPROVE_OPTION)
			return;
		File f = fc.getSelectedFile();

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
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("ui.export.error", ex.getMessage())));
			}
		}, AppExecutors.IO_EXECUTOR);
	}
}
