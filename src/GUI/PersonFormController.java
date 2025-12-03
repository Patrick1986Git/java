package GUI;

import model.Person;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller formularza Person - teraz showForm zwraca
 * CompletableFuture<Person>. Future zwraca obiekt Person gdy użytkownik kliknie
 * Zapisz, lub null gdy Anuluj.
 */
public class PersonFormController extends AbstractFormController<Person> {
	private final JDialog dialog;

	public PersonFormController(JFrame owner) {
		this.dialog = new JDialog(owner, "Person", true);
	}

	/**
	 * Pokaż formularz z danym obiektem. Zwraca future, który completes gdy
	 * użytkownik zatwierdzi/wyjdzie.
	 */
	public CompletableFuture<Person> showForm(Person p) {
		CompletableFuture<Person> result = new CompletableFuture<>();
		PersonForm form = new PersonForm((JFrame) dialog.getOwner(), new AbstractFormController<Person>() {
			@Override
			public void onSave(Person entity) {
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

		form.setEntity(p);

		dialog.getContentPane().removeAll();
		dialog.getContentPane().add(form);
		dialog.pack();
		dialog.setLocationRelativeTo(dialog.getOwner());

		// show modal in EDT
		SwingUtilities.invokeLater(() -> dialog.setVisible(true));

		return result;
	}

	// Nieużywane (zachowane dla kompatybilności)
	@Override
	public void onSave(Person entity) {
	}

	@Override
	public void onCancel() {
	}
}
