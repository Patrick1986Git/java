package GUI;

import model.Person;
import utils.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PersonForm extends AbstractForm<Person> {
	private final JTextField tfName = new JTextField(20);
	private final JTextField tfSurname = new JTextField(20);
	private final JSpinner spAge = new JSpinner(new SpinnerNumberModel(18, 0, 150, 1));
	private final JFormattedTextField tfDateOfBirth;
	private final JFormattedTextField tfStartDate;

	private Person entity;
	private final AbstractFormController<Person> controller;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public PersonForm(JFrame owner, AbstractFormController<Person> controller) {
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
	public void setEntity(Person entity) {
		this.entity = entity;
		if (entity != null) {
			tfName.setText(entity.getName());
			tfSurname.setText(entity.getSurname());
			spAge.setValue(entity.getAge() == null ? 18 : entity.getAge());
			tfDateOfBirth.setText(entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : "");
			tfStartDate.setText(entity.getStartDate() != null ? entity.getStartDate().toString() : "");
		}
	}

	@Override
	public Person getEntity() {
		if (entity == null)
			entity = new Person(null, "", "", 18, null, null) {
			};

		String name = tfName.getText();
		String surname = tfSurname.getText();
		int age = (Integer) spAge.getValue();
		String dobText = tfDateOfBirth.getText();
		String startText = tfStartDate.getText();

		// walidacja centralna
		ValidationResult vr = utils.validation.FormValidators.validatePersonInputs(name, surname, age, dobText,
				startText);
		if (!vr.isValid()) {
			JOptionPane.showMessageDialog(this, vr.getMessage(), "Błąd", JOptionPane.WARNING_MESSAGE);
			return null;
		}

		// parsowanie (już bezpieczne - walidacja przeszła)
		LocalDate dob = utils.validation.FormValidators.parseDateOrNull(dobText);
		LocalDate start = utils.validation.FormValidators.parseDateOrNull(startText);

		// ustaw w encji
		entity.setName(name.trim());
		entity.setSurname(surname.trim());
		entity.setAge(age);
		entity.setDateOfBirth(dob);
		entity.setStartDate(start);

		return entity;
	}

}
