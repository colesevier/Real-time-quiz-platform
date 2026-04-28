package com.quiz.model;

import java.util.UUID;

public class User {
    private UUID userID;
    private String username;
    private String passwordHash;
    private String email;
    private String displayName; // optional, shown instead of username
    private String bio;         // optional, up to 500 chars

    public User() {}

    public User(UUID userID, String username, String passwordHash, String email) {
        this.userID = userID;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public UUID getUserID() { return userID; }
    public void setUserID(UUID userID) { this.userID = userID; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    /** Returns displayName if set, otherwise falls back to username. */
    public String getEffectiveName() {
        return (displayName != null && !displayName.isBlank()) ? displayName : username;
    }
}