package service;

import model.User;
import repository.RoleRepository;
import repository.UserRepositoryImpl;
import security.AuthManager;
import utils.LoggerUtil;
import utils.SecurityUtil;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UserServiceImpl implements UserService {
	private final UserRepositoryImpl repo;
	private final RoleRepository roleRepo;

	public UserServiceImpl(UserRepositoryImpl repo, RoleRepository roleRepo) {
		this.repo = repo;
		this.roleRepo = roleRepo;
	}

	@Override
	public CompletableFuture<User> createUser(String username, char[] password, boolean enabled) {
		return createUser(username, password, enabled, false);
	}

	// overload: pozwala ustawić mustChange przy tworzeniu (użyteczne dla admina)
	@Override
	public CompletableFuture<User> createUser(String username, char[] password, boolean enabled, boolean mustChange) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				byte[] salt = SecurityUtil.generateSalt();
				byte[] hash = SecurityUtil.hashPassword(password, salt);
				User u = new User();
				u.setUsername(username);
				u.setSalt(salt);
				u.setPasswordHash(hash);
				u.setEnabled(enabled);
				u.setMustChangePassword(mustChange); // NEW
				u.setCreatedAt(LocalDate.now());
				u.setUpdatedAt(LocalDate.now());

				User saved = repo.save(u);
				LoggerUtil.log(java.util.logging.Level.INFO, "Created user: username=" + saved.getUsername()
						+ " by user=" + AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("CREATE_USER", "user:" + saved.getUsername(),
						"mustChange=" + saved.isMustChangePassword());
				return saved;
			} catch (Exception ex) {
				LoggerUtil.error("Create user failed", ex);
				throw new RuntimeException(ex);
			}
		}, utils.concurrent.AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Optional<User>> findByUsername(String username) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return repo.findByUsername(username);
			} catch (Exception ex) {
				LoggerUtil.error("Find user failed", ex);
				throw new RuntimeException(ex);
			}
		}, utils.concurrent.AppExecutors.DB_EXECUTOR);
	}

	@Override
	public CompletableFuture<Boolean> authenticate(String username, char[] password) {
		return findByUsername(username).thenApply(opt -> {
			if (opt.isEmpty())
				return false;
			User u = opt.get();
			if (!u.isEnabled())
				return false;
			return SecurityUtil.verifyPassword(password, u.getSalt(), u.getPasswordHash());
		});
	}

	@Override
	public CompletableFuture<Void> assignRole(String username, String roleName) {
		return findByUsername(username).thenAccept(opt -> {
			if (opt.isEmpty())
				throw new RuntimeException("User not found");
			User u = opt.get();
			try {
				Integer roleId = roleRepo.findIdByName(roleName);
				if (roleId == null)
					throw new RuntimeException("Role not found");
				repo.assignRole(u.getId(), roleId);
				LoggerUtil.log(java.util.logging.Level.INFO, "Assigned role " + roleName + " to user=" + username
						+ " by " + AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("ASSIGN_ROLE", "user:" + username, "role=" + roleName);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public CompletableFuture<Void> changePassword(String username, char[] newPassword) {
		return CompletableFuture.runAsync(() -> {
			try {
				byte[] salt = SecurityUtil.generateSalt();
				byte[] hash = SecurityUtil.hashPassword(newPassword, salt);
				repo.updatePasswordByUsername(username, hash, salt, false);
				LoggerUtil.log(java.util.logging.Level.INFO, "Password changed for user=" + username + " by "
						+ AuthManager.get().getCurrentUsernameOrSystem());
				LoggerUtil.audit("CHANGE_PASSWORD", "user:" + username,
						"changedBy=" + AuthManager.get().getCurrentUsernameOrSystem());
			} catch (Exception ex) {
				LoggerUtil.error("Change password failed", ex);
				throw new RuntimeException(ex);
			}
		}, utils.concurrent.AppExecutors.DB_EXECUTOR);
	}
}
