package security;

import model.User;

import java.util.Optional;

public final class AuthManager {
	private static final AuthManager INSTANCE = new AuthManager();
	private volatile User currentUser;

	private AuthManager() {
	}

	public static AuthManager get() {
		return INSTANCE;
	}

	public void setCurrentUser(User u) {
		this.currentUser = u;
	}

	public Optional<User> getCurrentUser() {
		return Optional.ofNullable(currentUser);
	}

	public void logout() {
		this.currentUser = null;
	}

	public boolean hasRole(String roleName) {
		if (currentUser == null)
			return false;
		return currentUser.getRoles().stream().anyMatch(r -> r.getName().equals(roleName));
	}

	/**
	 * Convenience: zwraca nazwę zalogowanego użytkownika lub "system" gdy brak.
	 * Używaj w logach/audicie żeby zawsze był kontekst.
	 */
	public String getCurrentUsernameOrSystem() {
		User u = currentUser;
		return u == null ? "system" : (u.getUsername() == null ? "system" : u.getUsername());
	}
}
