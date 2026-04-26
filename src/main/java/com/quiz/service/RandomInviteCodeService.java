package com.quiz.service;

import com.quiz.repository.GameSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.function.Predicate;

@Service
public class RandomInviteCodeService implements InviteCodeService {

    private static final String NUMERIC = "0123456789";
    // omit easily-confused chars (0/O, 1/I, etc.) for alphanumeric mode
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int MAX_RETRIES = 8;

    private final SecureRandom random = new SecureRandom();
    private final Predicate<String> existsCheck;
    private final int length;
    private final String alphabet;

    @Autowired
    public RandomInviteCodeService(GameSessionRepository repo,
                                   @Value("${quiz.lobby.code-length:6}") int length,
                                   @Value("${quiz.lobby.code-alphabet:NUMERIC}") String alphabetType) {
        this(repo::existsByCode, length, alphabetType);
    }

    // Constructor used by tests so we don't need a real DB
    RandomInviteCodeService(Predicate<String> existsCheck, int length, String alphabetType) {
        this.existsCheck = existsCheck;
        this.length = length;
        this.alphabet = "ALPHANUMERIC".equalsIgnoreCase(alphabetType) ? ALPHANUMERIC : NUMERIC;
    }

    @Override
    public String generate() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String code = randomCode();
            if (!existsCheck.test(code)) return code;
        }
        throw new IllegalStateException("Could not generate unique invite code after " + MAX_RETRIES + " attempts");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int j = 0; j < length; j++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }
}
