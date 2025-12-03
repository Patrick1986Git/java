package GUI;

import model.Employee;
import utils.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmployeeForm extends AbstractForm<Employee> {
	private final JTextField tfName = new JTextField(20);
	private final JTextField tfSurname = new JTextField(20);
	private final JSpinner spAge = new JSpinner(new SpinnerNumberModel(25, 0, 150, 1));
	private final JFormattedTextField tfDateOfBirth;
	private final JFormattedTextField tfStartDate;
	private final JSpinner spSalary = new JSpinner(new SpinnerNumberModel(3000.0, 0.0, 1_000_000.0, 100.0));
	private final JTextField tfPosition = new JTextField(20);

	private Employee entity;
	private final AbstractFormController<Employee> controller;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public EmployeeForm(JFrame owner, AbstractFormController<Employee> controller) {
		super(owner);
		this.controller = controller;

		tfDateOfBirth = new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd"));
		tfStartDate = new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd"));

		build();
	}

	private void build() {
		JPanel fields = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.fill = GridBagConstraints.HORIZONTAL;

		int y = 0;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Imię:"), c);
		c.gridx = 1;
		fields.add(tfName, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Nazwisko:"), c);
		c.gridx = 1;
		fields.add(tfSurname, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Wiek:"), c);
		c.gridx = 1;
		fields.add(spAge, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Data urodzenia (yyyy-MM-dd):"), c);
		c.gridx = 1;
		fields.add(tfDateOfBirth, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Data rozpoczęcia (yyyy-MM-dd):"), c);
		c.gridx = 1;
		fields.add(tfStartDate, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Pensja:"), c);
		c.gridx = 1;
		fields.add(spSalary, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Stanowisko:"), c);
		c.gridx = 1;
		fields.add(tfPosition, c);

		add(fields, BorderLayout.CENTER);

		JPanel buttons = new JPanel();
		JButton btnSave = new JButton("Zapisz");
		JButton btnCancel = new JButton("Anuluj");
		btnSave.addActionListener(e -> controller.onSave(getEntity()));
		btnCancel.addActionListener(e -> controller.onCancel());
		buttons.add(btnSave);
		buttons.add(btnCancel);
		add(buttons, BorderLayout.SOUTH);
	}

	@Override
	public void setEntity(Employee entity) {
		this.entity = entity;
		if (entity != null) {
			tfName.setText(entity.getName());
			tfSurname.setText(entity.getSurname());
			spAge.setValue(entity.getAge() == null ? 25 : entity.getAge());
			tfDateOfBirth.setText(entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : "");
			tfStartDate.setText(entity.getStartDate() != null ? entity.getStartDate().toString() : "");
			spSalary.setValue(entity.getSalary() == null ? 0.0 : entity.getSalary());
			tfPosition.setText(entity.getPosition());
		}
	}

	@Override
	public Employee getEntity() {
		if (entity == null)
			entity = new Employee(null, "", "", 25, null, null, 0.0, "");

		String name = tfName.getText().trim();
		String surname = tfSurname.getText().trim();
		String position = tfPosition.getText().trim();
		String dobText = tfDateOfBirth.getText().trim();
		String startText = tfStartDate.getText().trim();
		int age = (Integer) spAge.getValue();
		double salary = (Double) spSalary.getValue();

		// Walidacja centralna
		ValidationResult vr = utils.validation.FormValidators.validateEmployeeInputs(name, surname, age, dobText,
				startText, salary, position);
		if (!vr.isValid()) {
			JOptionPane.showMessageDialog(this, vr.getMessage(), "Błąd", JOptionPane.WARNING_MESSAGE);
			return null;
		}

		// Parsowanie dat (już bezpieczne)
		LocalDate dob = utils.validation.FormValidators.parseDateOrNull(dobText);
		LocalDate start = utils.validation.FormValidators.parseDateOrNull(startText);

		// Ustaw dane w encji
		entity.setName(name);
		entity.setSurname(surname);
		entity.setPosition(position);
		entity.setAge(age);
		entity.setSalary(salary);
		entity.setDateOfBirth(dob);
		entity.setStartDate(start);

		return entity;
	}

}
