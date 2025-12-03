package service;

import model.User;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserService {
    CompletableFuture<User> createUser(String username, char[] password, boolean enabled);
    CompletableFuture<User> createUser(String username, char[] password, boolean enabled, boolean mustChange);
    CompletableFuture<Optional<User>> findByUsername(String username);
    CompletableFuture<Boolean> authenticate(String username, char[] password);
    CompletableFuture<Void> assignRole(String username, String roleName);
    CompletableFuture<Void> changePassword(String username, char[] newPassword);
}
