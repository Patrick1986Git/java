package GUI;

import model.User;
import service.UserService;
import security.AuthManager;
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

    public LoginDialog(JFrame owner, UserService userService) {
        super(owner, "Login", true);
        this.userService = userService;
        build();
    }

    private void build() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        int y = 0;
        c.gridx = 0; c.gridy = y; p.add(new JLabel("User:"), c);
        c.gridx = 1; p.add(tfUser, c);

        y++;
        c.gridx = 0; c.gridy = y; p.add(new JLabel("Pass:"), c);
        c.gridx = 1; p.add(pf, c);

        JPanel buttons = new JPanel();
        JButton btnLogin = new JButton("Zaloguj");
        JButton btnCancel = new JButton("Anuluj");
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

    private void onLogin() {
        final String username = tfUser.getText().trim();
        final char[] password = pf.getPassword();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Podaj nazwę użytkownika", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password == null || password.length == 0) {
            JOptionPane.showMessageDialog(this, "Podaj hasło", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1) authenticate (asynchronicznie)
        userService.authenticate(username, password)
                .thenCompose(authOk -> {
                    if (!authOk) {
                        // wrong credentials -> show message on EDT and return false
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Niepoprawne dane"));
                        return CompletableFuture.completedFuture(false);
                    }
                    // 2) fetch full user record
                    return userService.findByUsername(username).thenCompose(optUser -> {
                        if (optUser.isEmpty()) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Nie znaleziono użytkownika po uwierzytelnieniu"));
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
                })
                .thenAccept(ok -> {
                    // clear password array asap
                    Arrays.fill(password, '\0');
                    if (ok) {
                        outcome.complete(true);
                        SwingUtilities.invokeLater(() -> setVisible(false));
                    } else {
                        // do not automatically close dialog on failure - allow retry
                        outcome.complete(false);
                    }
                })
                .exceptionally(ex -> {
                    Arrays.fill(password, '\0');
                    LoggerUtil.error("Login failed", ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd logowania: " + ex.getMessage()));
                    outcome.complete(false);
                    return null;
                });
    }

    /**
     * Pokazuje modalny panel do zmiany hasła (twoje hasło + potwierdzenie).
     * Jeśli użytkownik zatwierdzi i walidacja przejdzie, wywołuje userService.changePassword(...)
     * oraz po zmianie ładuje użytkownika i ustawia w AuthManager.
     *
     * Zwraca CompletableFuture<Boolean> z rezultatem (true = hasło zmienione i user ustawiony).
     */
    private CompletableFuture<Boolean> promptChangePasswordAndApply(String username) {
        // pokaż modalne okienko na EDT i pobierz wynik synchronnie:
        final String title = "Wymagana zmiana hasła";
        final JPasswordField newPf = new JPasswordField(20);
        final JPasswordField confirmPf = new JPasswordField(20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        int y = 0;
        c.gridx = 0; c.gridy = y; panel.add(new JLabel("Nowe hasło:"), c);
        c.gridx = 1; panel.add(newPf, c);
        y++;
        c.gridx = 0; c.gridy = y; panel.add(new JLabel("Powtórz hasło:"), c);
        c.gridx = 1; panel.add(confirmPf, c);

        // show dialog and block EDT (this method called from the CompletableFuture chain
        // on a worker thread; but we must show dialog on EDT and wait for user's input).
        final int[] option = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                option[0] = JOptionPane.showConfirmDialog(
                        this,
                        panel,
                        title,
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );
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
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Hasło musi mieć co najmniej 6 znaków", "Błąd", JOptionPane.WARNING_MESSAGE));
            Arrays.fill(newPass, '\0'); Arrays.fill(confirm, '\0');
            return CompletableFuture.completedFuture(false);
        }
        if (!Arrays.equals(newPass, confirm)) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Hasła nie są takie same", "Błąd", JOptionPane.WARNING_MESSAGE));
            Arrays.fill(newPass, '\0'); Arrays.fill(confirm, '\0');
            return CompletableFuture.completedFuture(false);
        }

        // call changePassword asynchronously (DB executor inside service)
        return userService.changePassword(username, newPass)
                .thenCompose(v -> {
                    // after change, reload user and set in AuthManager
                    return userService.findByUsername(username).thenApply(opt -> {
                        if (opt.isPresent()) {
                            AuthManager.get().setCurrentUser(opt.get());
                            return true;
                        } else {
                            return false;
                        }
                    });
                })
                .whenComplete((res, ex) -> {
                    // clear sensitive arrays
                    Arrays.fill(newPass, '\0'); Arrays.fill(confirm, '\0');
                    if (ex != null) {
                        LoggerUtil.error("After password change failed", ex);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd zmiany hasła: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE));
                    } else if (Boolean.TRUE.equals(res)) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Hasło zmienione pomyślnie"));
                    }
                });
    }

    public CompletableFuture<Boolean> showDialog() {
        SwingUtilities.invokeLater(() -> setVisible(true));
        return outcome;
    }
}
