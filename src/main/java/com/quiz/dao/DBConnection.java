package com.quiz.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static String url = System.getenv().getOrDefault("QUIZ_DB_URL", "jdbc:mysql://localhost:3306/kahoot?useSSL=false&serverTimezone=UTC");
    private static String user = System.getenv().getOrDefault("QUIZ_DB_USER", "root");
    private static String pass = System.getenv().getOrDefault("QUIZ_DB_PASS", "");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }
}
