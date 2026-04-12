-- ============================================================
-- kahoot database setup script
-- NOTE: To generate a bcrypt hash for the placeholder below,
--   use Java jBCrypt:  BCrypt.hashpw(plainPassword, BCrypt.gensalt(12))
--   or register a user via the app's registration endpoint which
--   hashes the password automatically before storing it.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Create database
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS kahoot
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 2. Create application user and grant privileges
-- ------------------------------------------------------------
CREATE USER IF NOT EXISTS 'quiz_user'@'localhost'
  IDENTIFIED BY 'REPLACE_WITH_SECURE_PASSWORD';

GRANT ALL PRIVILEGES ON kahoot.* TO 'quiz_user'@'localhost';
FLUSH PRIVILEGES;

-- ------------------------------------------------------------
-- 3. Use the database
-- ------------------------------------------------------------
USE kahoot;

-- ------------------------------------------------------------
-- 4. Table: users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    userID        BINARY(16)   NOT NULL,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    passwordHash  VARCHAR(255) NOT NULL,
    createdAt     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (userID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 5. Table: games
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games (
    gameID        INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    title         VARCHAR(255)  NOT NULL,
    description   TEXT,
    ownerID       BINARY(16)    NOT NULL,
    createdAt     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (gameID),
    CONSTRAINT fk_games_owner
        FOREIGN KEY (ownerID) REFERENCES users (userID)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 6. Table: questions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS questions (
    questionID    INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    gameID        INT UNSIGNED  NOT NULL,
    questionText  TEXT          NOT NULL,
    optionA       VARCHAR(500)  NOT NULL,
    optionB       VARCHAR(500)  NOT NULL,
    optionC       VARCHAR(500),
    optionD       VARCHAR(500),
    correctOption CHAR(1)       NOT NULL COMMENT 'A, B, C, or D',
    timeLimitSec  TINYINT UNSIGNED NOT NULL DEFAULT 30,
    points        SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    PRIMARY KEY (questionID),
    CONSTRAINT fk_questions_game
        FOREIGN KEY (gameID) REFERENCES games (gameID)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 7. Table: game_sessions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_sessions (
    sessionID     INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    gameID        INT UNSIGNED  NOT NULL,
    hostID        BINARY(16)    NOT NULL,
    inviteCode    VARCHAR(20)   NOT NULL UNIQUE,
    status        ENUM('waiting','active','finished') NOT NULL DEFAULT 'waiting',
    startedAt     DATETIME,
    endedAt       DATETIME,
    createdAt     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sessionID),
    CONSTRAINT fk_sessions_game
        FOREIGN KEY (gameID) REFERENCES games (gameID)
        ON DELETE CASCADE,
    CONSTRAINT fk_sessions_host
        FOREIGN KEY (hostID) REFERENCES users (userID)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. Sample data
-- ============================================================

-- Placeholder user  (UUID: 11111111-1111-1111-1111-111111111111)
-- Replace <bcrypt-hash> with the actual hash before using in production.
INSERT INTO users (userID, username, email, passwordHash, createdAt)
VALUES (
    UNHEX(REPLACE('11111111-1111-1111-1111-111111111111', '-', '')),
    'sample_user',
    'sample@example.com',
    '<bcrypt-hash>',
    NOW()
);

-- Sample game
INSERT INTO games (title, description, ownerID)
VALUES (
    'General Knowledge Quiz',
    'A fun general knowledge quiz for all ages.',
    UNHEX(REPLACE('11111111-1111-1111-1111-111111111111', '-', ''))
);

-- Two sample questions for gameID = 1
INSERT INTO questions (gameID, questionText, optionA, optionB, optionC, optionD, correctOption, timeLimitSec, points)
VALUES
(1, 'What is the capital of France?',
    'Berlin', 'Madrid', 'Paris', 'Rome',
    'C', 20, 100),

(1, 'Which planet is known as the Red Planet?',
    'Venus', 'Mars', 'Jupiter', 'Saturn',
    'B', 20, 100);

-- Sample game session with inviteCode 'ABC123'
INSERT INTO game_sessions (gameID, hostID, inviteCode, status)
VALUES (
    1,
    UNHEX(REPLACE('11111111-1111-1111-1111-111111111111', '-', '')),
    'ABC123',
    'waiting'
);