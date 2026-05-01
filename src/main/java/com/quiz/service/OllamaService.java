package com.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OllamaService {

    private final String baseUrl;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public List<Question> generateQuestions(int gameId, String extractedText, int count) throws Exception {
        String prompt = buildPrompt(extractedText, count);

        // Build request body
String body = mapper.writeValueAsString(
        mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", false)
                .put("num_predict", 300 * count) // ~300 tokens per question
);

        // Call Ollama API at /api/generate endpoint
        // Note: Ollama uses /api/generate (not /api/chat) for text generation
        URL url = new URL(baseUrl + "/api/generate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000); // 2 min — LLM can be slow

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        // Check for connection errors
        int statusCode = conn.getResponseCode();
        if (statusCode != 200) {
            String errorMsg = new String(conn.getErrorStream() != null 
                ? conn.getErrorStream().readAllBytes() 
                : "HTTP ".concat(String.valueOf(statusCode)).getBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Ollama API error (" + statusCode + "): " + errorMsg + 
                    "\nMake sure Ollama is running at " + baseUrl + " with model '" + model + "'");
        }

        String raw = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return parseResponse(gameId, raw, count);
    }

private String buildPrompt(String text, int count) {
    String truncated = text.length() > 4000 ? text.substring(0, 4000) : text;
    return """
            You are a quiz generator. Generate EXACTLY %d multiple choice questions. Not more, not fewer. EXACTLY %d.
            
            Rules:
            - Each question must have exactly 4 choices labeled A, B, C, D
            - Exactly one choice must be correct
            - Return ONLY a valid JSON array with EXACTLY %d elements, no explanation, no markdown, no backticks
            - Use this exact format:
            
            [
              {
                "question": "Question text here?",
                "A": "First choice",
                "B": "Second choice",
                "C": "Third choice",
                "D": "Fourth choice",
                "answer": "A"
              }
            ]
            
            Content:
            %s
            """.formatted(count, count, count, truncated);
}

private List<Question> parseResponse(int gameId, String raw, int count) throws Exception {
    // Handle empty response
    if (raw == null || raw.trim().isEmpty()) {
        throw new RuntimeException("Ollama returned empty response. Check that model '" + model + "' exists and Ollama is running.");
    }

    JsonNode root = mapper.readTree(raw);
    
    // Check for error in response
    if (root.has("error")) {
        throw new RuntimeException("Ollama error: " + root.path("error").asText());
    }
    
    String responseText = root.path("response").asText();
    
    if (responseText.isEmpty()) {
        throw new RuntimeException("Ollama did not generate a response. Model may be slow or unresponsive.");
    }

    // Strip markdown fences
    responseText = responseText
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("(?s)```\\s*", "")
            .trim();

    // Find start of array
    int start = responseText.indexOf('[');
    if (start == -1) {
        // If no array found, log raw response for debugging
        String preview = responseText.substring(0, Math.min(300, responseText.length()));
        throw new RuntimeException("No JSON array found in model response. Got: " + preview);
    }

    responseText = responseText.substring(start);

    // Find the last complete object and close the array there
    int lastBrace = responseText.lastIndexOf('}');
    if (lastBrace == -1) throw new RuntimeException("No complete question objects found in response");

    // Check if array closes properly after last brace
    int closingBracket = responseText.indexOf(']', lastBrace);
    if (closingBracket != -1) {
        // Array closed properly, take everything up to and including ]
        responseText = responseText.substring(0, closingBracket + 1);
    } else {
        // Array was cut off or has trailing text — close it manually
        responseText = responseText.substring(0, lastBrace + 1) + "]";
    }

    JsonNode array = mapper.readTree(responseText);
    
    if (!array.isArray()) {
        throw new RuntimeException("Expected JSON array, got: " + array.getNodeType());
    }
    
    List<Question> questions = new ArrayList<>();

    for (JsonNode node : array) {
        // Skip incomplete nodes missing required fields
        if (node.path("question").isMissingNode() || node.path("A").isMissingNode()) {
            continue;
        }

        String answer = node.path("answer").asText("A").toUpperCase().trim();
        if (!answer.matches("[ABCD]")) answer = "A";

        try {
            questions.add(new Question(
                    gameId,
                    node.path("question").asText(),
                    answer.charAt(0),
                    node.path("A").asText(),
                    node.path("B").asText(),
                    node.path("C").asText(),
                    node.path("D").asText()
            ));
        } catch (Exception e) {
            // Skip malformed questions
            continue;
        }
    }
    
    if (questions.isEmpty()) {
        throw new RuntimeException("No valid questions could be parsed from Ollama response");
    }
    
    return questions.stream().limit(count).collect(java.util.stream.Collectors.toList());
}
}