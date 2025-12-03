package GUI;

import model.Employee;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller formularza Employee - showForm zwraca CompletableFuture<Employee>.
 */
public class EmployeeFormController extends AbstractFormController<Employee> {
	private final JDialog dialog;

	public EmployeeFormController(JFrame owner) {
		this.dialog = new JDialog(owner, "Employee", true);
	}

	public CompletableFuture<Employee> showForm(Employee e) {
		CompletableFuture<Employee> result = new CompletableFuture<>();
		EmployeeForm form = new EmployeeForm((JFrame) dialog.getOwner(), new AbstractFormController<Employee>() {
			@Override
			public void onSave(Employee entity) {
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
	public void onSave(Employee entity) {
	}

	@Override
	public void onCancel() {
	}
}
