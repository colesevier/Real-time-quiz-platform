package com.quiz.repository;

import com.quiz.model.Game;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GameRepository {

    private final JdbcTemplate jdbc;

    public GameRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(UUID userID, String title) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO games (userID, gameTitle) VALUES (UNHEX(REPLACE(?,'-','')), ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, userID.toString());
            ps.setString(2, title);
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).intValue();
    }

    public Optional<Game> findById(int gameID) {
        return jdbc.query(
                "SELECT gameID, HEX(userID) AS userID, gameTitle, createdAt, lastPlayedAt " +
                        "FROM games WHERE gameID = ?",
                rs -> rs.next() ? Optional.of(MAPPER.mapRow(rs, 1)) : Optional.<Game>empty(),
                gameID);
    }

    public List<Game> findByUser(UUID userID) {
        return jdbc.query(
                "SELECT gameID, HEX(userID) AS userID, gameTitle, createdAt, lastPlayedAt " +
                        "FROM games WHERE userID = UNHEX(REPLACE(?,'-','')) ORDER BY createdAt DESC",
                MAPPER, userID.toString());
    }

    private static final RowMapper<Game> MAPPER = (rs, i) -> {
        Game g = new Game();
        g.setGameID(rs.getInt("gameID"));
        g.setUserID(uuidFromHex(rs.getString("userID")));
        g.setGameTitle(rs.getString("gameTitle"));
        g.setCreatedAt(rs.getTimestamp("createdAt") == null ? null : rs.getTimestamp("createdAt").toInstant());
        g.setLastPlayedAt(rs.getTimestamp("lastPlayedAt") == null ? null : rs.getTimestamp("lastPlayedAt").toInstant());
        return g;
    };

    static UUID uuidFromHex(String hex) {
        StringBuilder s = new StringBuilder(hex.toLowerCase());
        s.insert(8, '-'); s.insert(13, '-'); s.insert(18, '-'); s.insert(23, '-');
        return UUID.fromString(s.toString());
    }
}
