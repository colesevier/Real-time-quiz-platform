package com.quiz.repository;

import com.quiz.model.Question;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionRepository {

    private final JdbcTemplate jdbc;

    public QuestionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Question q) {
        return jdbc.update(
                "INSERT INTO questions (gameID, questionPrompt, correctAnswer, choiceA, choiceB, choiceC, choiceD) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                q.getGameID(), q.getQuestionPrompt(), String.valueOf(q.getCorrectAnswer()),
                q.getChoiceA(), q.getChoiceB(), q.getChoiceC(), q.getChoiceD());
    }

    public List<Question> findByGame(int gameID) {
        return jdbc.query(
                "SELECT questionID, gameID, questionPrompt, correctAnswer, choiceA, choiceB, choiceC, choiceD " +
                        "FROM questions WHERE gameID = ? ORDER BY questionID",
                MAPPER, gameID);
    }

    public int countByGame(int gameID) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM questions WHERE gameID = ?", Integer.class, gameID);
        return n == null ? 0 : n;
    }

    private static final RowMapper<Question> MAPPER = (rs, i) -> {
        Question q = new Question();
        q.setQuestionID(rs.getInt("questionID"));
        q.setGameID(rs.getInt("gameID"));
        q.setQuestionPrompt(rs.getString("questionPrompt"));
        String c = rs.getString("correctAnswer");
        q.setCorrectAnswer(c == null || c.isEmpty() ? '?' : c.charAt(0));
        q.setChoiceA(rs.getString("choiceA"));
        q.setChoiceB(rs.getString("choiceB"));
        q.setChoiceC(rs.getString("choiceC"));
        q.setChoiceD(rs.getString("choiceD"));
        return q;
    };
}
