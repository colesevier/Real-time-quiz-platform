package com.quiz.service;

import java.time.Instant;

public record Player(String stompSessionId, String nickname, Instant joinedAt) {}
