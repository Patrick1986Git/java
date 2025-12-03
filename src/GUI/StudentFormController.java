package GUI;

import model.Student;
import service.StudentService;
import utils.LoggerUtil;

import java.util.concurrent.CompletableFuture;

import javax.swing.*;

public class StudentFormController extends AbstractFormController<Student> {

	private final JDialog dialog;

	public StudentFormController(JFrame owner) {

		this.dialog = new JDialog(owner, "Student", true);
	}

	public CompletableFuture<Student> showForm(Student e) {
		CompletableFuture<Student> result = new CompletableFuture<>();
		StudentForm form = new StudentForm((JFrame) dialog.getOwner(), new AbstractFormController<Student>() {
			@Override
			public void onSave(Student entity) {
				// Jeśli walidacja zwróciła null → nie zamykaj
				if (entity == null)
					return;
				result.complete(entity);
				dialog.setVisible(false);
			}

			@Override
			public void onCancel() {
				result.complete(null);
				dialog.setVisible(false);
			}
		});

		form.setEntity(e);

		dialog.getContentPane().removeAll();
		dialog.getContentPane().add(form);
		dialog.pack();
		dialog.setLocationRelativeTo(dialog.getOwner());

		SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		return result;
	}

	@Override
	public void onSave(Student entity) {
	}

	@Override
	public void onCancel() {
	}
}
