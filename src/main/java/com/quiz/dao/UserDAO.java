package com.quiz.dao;

import com.quiz.model.User;
import com.quiz.util.PasswordUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDAO {

    private final JdbcTemplate jdbc;

    public UserDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean register(User u) {
        String sql = "INSERT INTO users (userID, username, email, passwordHash) " +
                "VALUES (UNHEX(REPLACE(?,'-','')), ?, ?, ?)";
        return jdbc.update(sql,
                u.getUserID().toString(),
                u.getUsername(),
                u.getEmail(),
                u.getPasswordHash()) == 1;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT HEX(userID) AS userID, username, email, passwordHash " +
                "FROM users WHERE username = ?";
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.<User>empty();
            UUID id = UUID.fromString(idFromHex(rs.getString("userID")));
            return Optional.of(new User(id,
                    rs.getString("username"),
                    rs.getString("passwordHash"),
                    rs.getString("email")));
        }, username);
    }

    public boolean validateCredentials(String username, String password) {
        return findByUsername(username)
                .map(u -> PasswordUtil.verify(password, u.getPasswordHash()))
                .orElse(false);
    }

    private String idFromHex(String hex) {
        StringBuilder s = new StringBuilder(hex.toLowerCase());
        s.insert(8, '-'); s.insert(13, '-'); s.insert(18, '-'); s.insert(23, '-');
        return s.toString();
    }
}
