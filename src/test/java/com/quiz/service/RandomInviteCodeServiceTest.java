package com.quiz.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomInviteCodeServiceTest {

    @Test
    void generates_six_numeric_digits_by_default() {
        Predicate<String> neverExists = code -> false;
        RandomInviteCodeService svc = new RandomInviteCodeService(neverExists, 6, "NUMERIC");
        String code = svc.generate();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(c -> c >= '0' && c <= '9'),
                "code should be all digits, got: " + code);
    }

    @Test
    void retries_on_collision_then_succeeds() {
        AtomicInteger calls = new AtomicInteger();
        Predicate<String> firstThreeCollide = code -> calls.incrementAndGet() <= 3;
        RandomInviteCodeService svc = new RandomInviteCodeService(firstThreeCollide, 6, "NUMERIC");
        String code = svc.generate();
        assertNotNull(code);
        assertEquals(4, calls.get(), "should have probed 4 codes before accepting");
    }

    @Test
    void throws_when_all_attempts_collide() {
        Predicate<String> alwaysExists = code -> true;
        RandomInviteCodeService svc = new RandomInviteCodeService(alwaysExists, 6, "NUMERIC");
        assertThrows(IllegalStateException.class, svc::generate);
    }

    @Test
    void alphanumeric_mode_uses_uppercase_alphabet() {
        Predicate<String> neverExists = code -> false;
        RandomInviteCodeService svc = new RandomInviteCodeService(neverExists, 6, "ALPHANUMERIC");
        // Exercise enough generations to be confident at least one non-digit appears
        Set<Character> chars = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            for (char c : svc.generate().toCharArray()) chars.add(c);
        }
        assertTrue(chars.stream().anyMatch(Character::isLetter),
                "alphanumeric mode should produce letters, saw: " + chars);
    }
}
