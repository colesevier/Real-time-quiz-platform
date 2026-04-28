package com.quiz.dao;

import com.quiz.model.User;
import com.quiz.util.PasswordUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles profile reads and updates for authenticated users.
 * Follows the same JdbcTemplate pattern as UserDAO.
 *
 * Requires these columns on the users table (run the migration SQL first):
 *   ALTER TABLE users
 *     ADD COLUMN displayName VARCHAR(100) DEFAULT NULL,
 *     ADD COLUMN bio         VARCHAR(500) DEFAULT NULL;
 */
@Repository
public class ProfileDAO {

    private final JdbcTemplate jdbc;

    public ProfileDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Read ────────────────────────────────────────────────────────────────

    /**
     * Load a user's full profile by their UUID.
     * Returns empty if the user doesn't exist (shouldn't happen for logged-in users,
     * but safe to handle).
     */
    public Optional<User> findById(UUID userID) {
        String sql = "SELECT HEX(userID) AS userID, username, email, passwordHash, " +
                     "displayName, bio " +
                     "FROM users WHERE userID = UNHEX(REPLACE(?, '-', ''))";
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.<User>empty();
            User u = new User();
            u.setUserID(uuidFromHex(rs.getString("userID")));
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setPasswordHash(rs.getString("passwordHash"));
            u.setDisplayName(rs.getString("displayName"));
            u.setBio(rs.getString("bio"));
            return Optional.of(u);
        }, userID.toString());
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Update display name and bio. Both are optional (nullable).
     * Returns true if the row was updated.
     */
    public boolean updateProfile(UUID userID, String displayName, String bio) {
        String sql = "UPDATE users SET displayName = ?, bio = ? " +
                     "WHERE userID = UNHEX(REPLACE(?, '-', ''))";
        return jdbc.update(sql,
                displayName == null || displayName.isBlank() ? null : displayName.trim(),
                bio         == null || bio.isBlank()         ? null : bio.trim(),
                userID.toString()) == 1;
    }

    /**
     * Change password after verifying the current one.
     * Returns true on success, false if the old password didn't match.
     */
    public boolean changePassword(UUID userID, String oldPassword, String newPassword) {
        // First fetch the current hash to verify
        String fetchSql = "SELECT passwordHash FROM users " +
                          "WHERE userID = UNHEX(REPLACE(?, '-', ''))";
        String currentHash = jdbc.query(fetchSql, rs -> {
            if (!rs.next()) return null;
            return rs.getString("passwordHash");
        }, userID.toString());

        if (currentHash == null || !PasswordUtil.verify(oldPassword, currentHash)) {
            return false; // old password wrong
        }

        String updateSql = "UPDATE users SET passwordHash = ? " +
                           "WHERE userID = UNHEX(REPLACE(?, '-', ''))";
        return jdbc.update(updateSql,
                PasswordUtil.hash(newPassword),
                userID.toString()) == 1;
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static UUID uuidFromHex(String hex) {
        StringBuilder s = new StringBuilder(hex.toLowerCase());
        s.insert(8, '-'); s.insert(13, '-'); s.insert(18, '-'); s.insert(23, '-');
        return UUID.fromString(s.toString());
    }
}