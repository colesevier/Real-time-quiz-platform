package com.quiz.repository;

import com.quiz.model.GameSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GameSessionRepository {

    private final JdbcTemplate jdbc;

    public GameSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsByCode(String code) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM game_sessions WHERE inviteCode = ?", Integer.class, code);
        return n != null && n > 0;
    }

    public Optional<GameSession> findByCode(String code) {
        return jdbc.query(
                "SELECT sessionID, gameID, inviteCode, status, timePerQuestion, maxPlayers, startedAt " +
                        "FROM game_sessions WHERE inviteCode = ?",
                rs -> rs.next() ? Optional.of(MAPPER.mapRow(rs, 1)) : Optional.<GameSession>empty(),
                code);
    }

    public int insert(int gameID, String inviteCode, int timePerQuestion, int maxPlayers) {
        return jdbc.update(
                "INSERT INTO game_sessions (gameID, inviteCode, status, timePerQuestion, maxPlayers) " +
                        "VALUES (?, ?, 'LOBBY', ?, ?)",
                gameID, inviteCode, timePerQuestion, maxPlayers);
    }

    public int updateStatus(String inviteCode, String status) {
        return jdbc.update(
                "UPDATE game_sessions SET status = ?, " +
                        "startedAt = CASE WHEN ? = 'ACTIVE' AND startedAt IS NULL THEN CURRENT_TIMESTAMP ELSE startedAt END " +
                        "WHERE inviteCode = ?",
                status, status, inviteCode);
    }

    private static final RowMapper<GameSession> MAPPER = (rs, i) -> {
        GameSession s = new GameSession();
        s.setSessionID(rs.getInt("sessionID"));
        s.setGameID(rs.getInt("gameID"));
        s.setInviteCode(rs.getString("inviteCode"));
        s.setStatus(rs.getString("status"));
        s.setTimePerQuestion(rs.getInt("timePerQuestion"));
        s.setMaxPlayers(rs.getInt("maxPlayers"));
        s.setStartedAt(rs.getTimestamp("startedAt") == null ? null : rs.getTimestamp("startedAt").toInstant());
        return s;
    };
}
