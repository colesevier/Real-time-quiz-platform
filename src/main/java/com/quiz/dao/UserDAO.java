package com.quiz.dao;

import com.quiz.model.User;
import com.quiz.util.PasswordUtil;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserDAO {

    public boolean register(User u) throws SQLException {
        String sql = "INSERT INTO users (userID, username, email, passwordHash) VALUES (UNHEX(REPLACE(?,'-','')),?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUserID().toString());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPasswordHash());
            return ps.executeUpdate() == 1;
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT HEX(userID) as userID, username, email, passwordHash FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String idHex = rs.getString("userID");
                    UUID id = UUID.fromString(idFromHex(idHex));
                    User u = new User(id, rs.getString("username"), rs.getString("passwordHash"), rs.getString("email"));
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }

    private String idFromHex(String hex) {
        // MySQL HEX( ) of a UUID stored as BINARY(16) produces a 32-char hex without dashes.
        // Convert it into UUID string format by inserting dashes.
        StringBuilder s = new StringBuilder(hex.toLowerCase());
        s.insert(8, '-'); s.insert(13, '-'); s.insert(18, '-'); s.insert(23, '-');
        return s.toString();
    }

    public boolean validateCredentials(String username, String password) throws SQLException {
        Optional<User> ou = findByUsername(username);
        if (ou.isEmpty()) return false;
        User u = ou.get();
        return PasswordUtil.verify(password, u.getPasswordHash());
    }
}
