package com.quiz.dto;

import java.util.List;

public class ManualGameRequest {
    private String title;
    private List<QuestionInput> questions;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<QuestionInput> getQuestions() { return questions; }
    public void setQuestions(List<QuestionInput> questions) { this.questions = questions; }

    public static class QuestionInput {
        private String prompt;
        private String a;
        private String b;
        private String c;
        private String d;
        private char correct;

        // Getters and Setters
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getA() { return a; }
        public void setA(String a) { this.a = a; }
        public String getB() { return b; }
        public void setB(String b) { this.b = b; }
        public String getC() { return c; }
        public void setC(String c) { this.c = c; }
        public String getD() { return d; }
        public void setD(String d) { this.d = d; }
        public char getCorrect() { return correct; }
        public void setCorrect(char correct) { this.correct = correct; }
    }
}