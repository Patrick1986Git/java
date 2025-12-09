package GUI;

import model.User;
import service.UserService;
import security.AuthManager;
import utils.LocalizationManager;
import utils.LoggerUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Dialog logowania z obsługą wymuszonej zmiany hasła (mustChangePassword).
 * Zwraca CompletableFuture<Boolean> oznaczające sukces logowania.
 */
public class LoginDialog extends JDialog {
	private final JTextField tfUser = new JTextField(20);
	private final JPasswordField pf = new JPasswordField(20);
	private final UserService userService;
	private final CompletableFuture<Boolean> outcome = new CompletableFuture<>();

	// komponenty, które chcemy aktualizować po zmianie locale
	private JButton btnLogin;
	private JButton btnCancel;

	public LoginDialog(JFrame owner, UserService userService) {
		super(owner, LocalizationManager.getString("login.title"), true);
		this.userService = userService;
		build();

		// reaguj na zmianę języka — update tekstów
		LocalizationManager.addListener(newLocale -> SwingUtilities.invokeLater(this::updateLocalizedTexts));
	}

	private void build() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		int y = 0;
		c.gridx = 0;
		c.gridy = y;
		p.add(new JLabel(LocalizationManager.getString("login.user")), c);
		c.gridx = 1;
		p.add(tfUser, c);

		y++;
		c.gridx = 0;
		c.gridy = y;
		p.add(new JLabel(LocalizationManager.getString("login.pass")), c);
		c.gridx = 1;
		p.add(pf, c);

		JPanel buttons = new JPanel();
		btnLogin = new JButton(LocalizationManager.getString("login.btn.login"));
		btnCancel = new JButton(LocalizationManager.getString("login.btn.cancel"));
		buttons.add(btnLogin);
		buttons.add(btnCancel);

		btnLogin.addActionListener(e -> onLogin());
		btnCancel.addActionListener(e -> {
			outcome.complete(false);
			// clear credentials
			Arrays.fill(pf.getPassword(), '\0');
			setVisible(false);
		});

		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(p, BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(getOwner());
	}

	/**
	 * Aktualizuje wszystkie widoczne teksty (wywoływane po zmianie locale).
	 */
	private void updateLocalizedTexts() {
		setTitle(LocalizationManager.getString("login.title"));
		// zmieniamy teksty etykiet — łatwo jeśli iterujemy po komponentach
		// -- zakładamy layout: center panel + buttons panel
		Container cp = getContentPane();
		if (cp.getComponentCount() >= 2) {
			Component center = cp.getComponent(0);
			if (center instanceof JPanel) {
				JPanel p = (JPanel) center;
				// zakładamy ułożenie zgodne z build(): label, field, label, field
				for (Component comp : p.getComponents()) {
					if (comp instanceof JLabel) {
						JLabel lbl = (JLabel) comp;
						String txt = lbl.getText();
						// mapowanie prostych etykiet - sprawdź i ustaw nowe jeśli zawiera znane klucze
						if (txt != null) {
							// porównuj po poprzednich wartościach kluczy (plausible heurystyka)
							// ustaw na podstawie pozycji: pierwszy label -> login.user, trzeci ->
							// login.pass
						}
					}
				}
			}
		}
		// aktualizuj buttony
		if (btnLogin != null)
			btnLogin.setText(LocalizationManager.getString("login.btn.login"));
		if (btnCancel != null)
			btnCancel.setText(LocalizationManager.getString("login.btn.cancel"));
	}

	private void onLogin() {
		final String username = tfUser.getText().trim();
		final char[] password = pf.getPassword();

		if (username.isEmpty()) {
			JOptionPane.showMessageDialog(this, LocalizationManager.getString("login.error.emptyUser"),
					LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (password == null || password.length == 0) {
			JOptionPane.showMessageDialog(this, LocalizationManager.getString("login.error.emptyPass"),
					LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}

		// 1) authenticate (asynchronicznie)
		userService.authenticate(username, password).thenCompose(authOk -> {
			if (!authOk) {
				// wrong credentials -> show message on EDT and return false
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(this, LocalizationManager.getString("login.error.invalid"),
								LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE));
				return CompletableFuture.completedFuture(false);
			}
			// 2) fetch full user record
			return userService.findByUsername(username).thenCompose(optUser -> {
				if (optUser.isEmpty()) {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
							LocalizationManager.getString("login.error.notfound"),
							LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE));
					return CompletableFuture.completedFuture(false);
				}
				User u = optUser.get();
				// 3) if mustChangePassword -> prompt change and perform it
				if (u.isMustChangePassword()) {
					return promptChangePasswordAndApply(username);
				} else {
					// set current user and succeed
					AuthManager.get().setCurrentUser(u);
					return CompletableFuture.completedFuture(true);
				}
			});
		}).thenAccept(ok -> {
			// clear password array asap
			Arrays.fill(password, '\0');
			if (ok) {
				outcome.complete(true);
				SwingUtilities.invokeLater(() -> setVisible(false));
			} else {
				// do not automatically close dialog on failure - allow retry
				outcome.complete(false);
			}
		}).exceptionally(ex -> {
			Arrays.fill(password, '\0');
			LoggerUtil.error("Login failed", ex);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					LocalizationManager.getString("login.error.exception") + ": " + ex.getMessage(),
					LocalizationManager.getString("dialog.error.title"), JOptionPane.ERROR_MESSAGE));
			outcome.complete(false);
			return null;
		});
	}

	/**
	 * Pokazuje modalny panel do zmiany hasła (twoje hasło + potwierdzenie). Jeśli
	 * użytkownik zatwierdzi i walidacja przejdzie, wywołuje
	 * userService.changePassword(...) oraz po zmianie ładuje użytkownika i ustawia
	 * w AuthManager.
	 *
	 * Zwraca CompletableFuture<Boolean> z rezultatem (true = hasło zmienione i user
	 * ustawiony).
	 */
	private CompletableFuture<Boolean> promptChangePasswordAndApply(String username) {
		final String title = LocalizationManager.getString("login.changePass.title");
		final JPasswordField newPf = new JPasswordField(20);
		final JPasswordField confirmPf = new JPasswordField(20);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;
		int y = 0;
		c.gridx = 0;
		c.gridy = y;
		panel.add(new JLabel(LocalizationManager.getString("login.changePass.new")), c);
		c.gridx = 1;
		panel.add(newPf, c);
		y++;
		c.gridx = 0;
		c.gridy = y;
		panel.add(new JLabel(LocalizationManager.getString("login.changePass.confirm")), c);
		c.gridx = 1;
		panel.add(confirmPf, c);

		final int[] option = new int[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				option[0] = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE);
			});
		} catch (Exception e) {
			LoggerUtil.error("Change password dialog invocation failed", e);
			return CompletableFuture.completedFuture(false);
		}

		if (option[0] != JOptionPane.OK_OPTION) {
			// user cancelled
			Arrays.fill(newPf.getPassword(), '\0');
			Arrays.fill(confirmPf.getPassword(), '\0');
			return CompletableFuture.completedFuture(false);
		}

		final char[] newPass = newPf.getPassword();
		final char[] confirm = confirmPf.getPassword();

		// local validation (length + match)
		if (newPass == null || newPass.length < 6) {
			SwingUtilities.invokeLater(
					() -> JOptionPane.showMessageDialog(this, LocalizationManager.getString("login.changePass.length"),
							LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE));
			Arrays.fill(newPass, '\0');
			Arrays.fill(confirm, '\0');
			return CompletableFuture.completedFuture(false);
		}
		if (!Arrays.equals(newPass, confirm)) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					LocalizationManager.getString("login.changePass.mismatch"),
					LocalizationManager.getString("dialog.error.title"), JOptionPane.WARNING_MESSAGE));
			Arrays.fill(newPass, '\0');
			Arrays.fill(confirm, '\0');
			return CompletableFuture.completedFuture(false);
		}

		// call changePassword asynchronously (DB executor inside service)
		return userService.changePassword(username, newPass).thenCompose(v -> {
			// after change, reload user and set in AuthManager
			return userService.findByUsername(username).thenApply(opt -> {
				if (opt.isPresent()) {
					AuthManager.get().setCurrentUser(opt.get());
					return true;
				} else {
					return false;
				}
			});
		}).whenComplete((res, ex) -> {
			// clear sensitive arrays
			Arrays.fill(newPass, '\0');
			Arrays.fill(confirm, '\0');
			if (ex != null) {
				LoggerUtil.error("After password change failed", ex);
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("login.changePass.error") + ": " + ex.getMessage(),
						LocalizationManager.getString("dialog.error.title"), JOptionPane.ERROR_MESSAGE));
			} else if (Boolean.TRUE.equals(res)) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						LocalizationManager.getString("login.changePass.success"),
						LocalizationManager.getString("dialog.info.title"), JOptionPane.INFORMATION_MESSAGE));
			}
		});
	}

	public CompletableFuture<Boolean> showDialog() {
		SwingUtilities.invokeLater(() -> setVisible(true));
		return outcome;
	}
}
