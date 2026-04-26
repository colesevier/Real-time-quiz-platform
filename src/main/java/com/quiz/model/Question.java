package com.quiz.model;

public class Question {
    private int questionID;
    private int gameID;
    private String questionPrompt;
    private char correctAnswer;
    private String choiceA;
    private String choiceB;
    private String choiceC;
    private String choiceD;

    public Question() {}

    public Question(int gameID, String questionPrompt, char correctAnswer,
                    String choiceA, String choiceB, String choiceC, String choiceD) {
        this.gameID = gameID;
        this.questionPrompt = questionPrompt;
        this.correctAnswer = correctAnswer;
        this.choiceA = choiceA;
        this.choiceB = choiceB;
        this.choiceC = choiceC;
        this.choiceD = choiceD;
    }

    public int getQuestionID() { return questionID; }
    public void setQuestionID(int questionID) { this.questionID = questionID; }

    public int getGameID() { return gameID; }
    public void setGameID(int gameID) { this.gameID = gameID; }

    public String getQuestionPrompt() { return questionPrompt; }
    public void setQuestionPrompt(String questionPrompt) { this.questionPrompt = questionPrompt; }

    public char getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(char correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getChoiceA() { return choiceA; }
    public void setChoiceA(String choiceA) { this.choiceA = choiceA; }

    public String getChoiceB() { return choiceB; }
    public void setChoiceB(String choiceB) { this.choiceB = choiceB; }

    public String getChoiceC() { return choiceC; }
    public void setChoiceC(String choiceC) { this.choiceC = choiceC; }

    public String getChoiceD() { return choiceD; }
    public void setChoiceD(String choiceD) { this.choiceD = choiceD; }
}
