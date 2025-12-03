package GUI;

import model.Student;
import utils.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StudentForm extends AbstractForm<Student> {
	private final JTextField tfName = new JTextField(20);
	private final JTextField tfSurname = new JTextField(20);
	private final JSpinner spAge = new JSpinner(new SpinnerNumberModel(20, 16, 150, 1));
	private final JFormattedTextField tfDateOfBirth;
	private final JFormattedTextField tfStartDate;
	private final JTextField tfUniversity = new JTextField(20);
	private final JSpinner spYear = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));

	private Student entity;
	private final AbstractFormController<Student> controller;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public StudentForm(JFrame owner, AbstractFormController<Student> controller) {
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
		fields.add(new JLabel("Uczelnia:"), c);
		c.gridx = 1;
		fields.add(tfUniversity, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		fields.add(new JLabel("Rok:"), c);
		c.gridx = 1;
		fields.add(spYear, c);

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
	public void setEntity(Student entity) {
		this.entity = entity;
		if (entity != null) {
			tfName.setText(entity.getName());
			tfSurname.setText(entity.getSurname());
			spAge.setValue(entity.getAge() == null ? 20 : entity.getAge());
			tfDateOfBirth.setText(entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : "");
			tfStartDate.setText(entity.getStartDate() != null ? entity.getStartDate().toString() : "");
			tfUniversity.setText(entity.getUniversity());
			spYear.setValue(entity.getYear() == null ? 1 : entity.getYear());
		}
	}

	@Override
	public Student getEntity() {
	    if (entity == null)
	        entity = new Student(null, "", "", 20, null, null, "", 1);

	    String name = tfName.getText().trim();
	    String surname = tfSurname.getText().trim();
	    String university = tfUniversity.getText().trim();
	    String dobText = tfDateOfBirth.getText().trim();
	    String startText = tfStartDate.getText().trim();
	    int age = (Integer) spAge.getValue();
	    int year = (Integer) spYear.getValue();

	    // Walidacja centralna
		ValidationResult vr = utils.validation.FormValidators.validateStudentInputs(
	            name, surname, age, dobText, startText, university, year);
	    if (!vr.isValid()) {
	        JOptionPane.showMessageDialog(this, vr.getMessage(), "Błąd", JOptionPane.WARNING_MESSAGE);
	        return null;
	    }

	    LocalDate dob = utils.validation.FormValidators.parseDateOrNull(dobText);
	    LocalDate start = utils.validation.FormValidators.parseDateOrNull(startText);

	    // Ustaw dane
	    entity.setName(name);
	    entity.setSurname(surname);
	    entity.setAge(age);
	    entity.setUniversity(university);
	    entity.setYear(year);
	    entity.setDateOfBirth(dob);
	    entity.setStartDate(start);

	    return entity;
	}

}
