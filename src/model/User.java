package model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class User {
	private Integer id;
	private String username;
	private byte[] passwordHash;
	private byte[] salt;
	private boolean enabled = true;
	private LocalDate createdAt;
	private LocalDate updatedAt;
	private boolean mustChangePassword = false; 
	private Set<Role> roles = new HashSet<>();

	public User() {
	}

	public User(Integer id, String username, byte[] passwordHash, byte[] salt) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.salt = salt;
	}

	// getters / setters (plus new)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public byte[] getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(byte[] passwordHash) {
		this.passwordHash = passwordHash;
	}

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public LocalDate getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDate createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDate getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDate updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	} // NEW

	public void setMustChangePassword(boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	} // NEW

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}
}
