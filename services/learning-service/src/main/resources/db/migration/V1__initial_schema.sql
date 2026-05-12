-- V1: Initial schema for Nihongo IT
-- Covers all entities shared across user-service, learning-service, notification-service

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- ROLES
-- =============================================
CREATE TABLE IF NOT EXISTS roles (
    role_id   INT          PRIMARY KEY,
    role_name VARCHAR(20)  NOT NULL
);

-- =============================================
-- USERS
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    user_id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email                    VARCHAR(50)  NOT NULL UNIQUE,
    password                 VARCHAR(255) NOT NULL,
    full_name                VARCHAR(100) NOT NULL,
    profile_picture          VARCHAR(255),
    current_level            VARCHAR(10),
    jlpt_goal                VARCHAR(10),
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    is_email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_token       VARCHAR(255),
    reset_password_token     VARCHAR(255),
    reset_password_expires   TIMESTAMP,
    last_login               TIMESTAMP,
    streak_count             INT          NOT NULL DEFAULT 0,
    last_study_date          TIMESTAMP,
    points                   INT          NOT NULL DEFAULT 0,
    daily_goal_minutes       INT          NOT NULL DEFAULT 15,
    reminder_enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    reminder_time            TIME                  DEFAULT '20:00:00',
    notification_preferences TEXT                  DEFAULT 'email,app',
    min_card_threshold       INT                   DEFAULT 5,
    firebase_token           VARCHAR(255),
    role_id                  INT          NOT NULL REFERENCES roles(role_id),
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================
-- CATEGORIES
-- =============================================
CREATE TABLE IF NOT EXISTS categories (
    category_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL UNIQUE,
    meaning       VARCHAR(255),
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP             DEFAULT NOW(),
    updated_at    TIMESTAMP             DEFAULT NOW()
);

-- =============================================
-- TOPICS
-- =============================================
CREATE TABLE IF NOT EXISTS topics (
    topic_id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL UNIQUE,
    meaning       VARCHAR(255) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    category_id   UUID         NOT NULL REFERENCES categories(category_id),
    created_at    TIMESTAMP             DEFAULT NOW(),
    updated_at    TIMESTAMP             DEFAULT NOW()
);

-- =============================================
-- VOCABULARY
-- =============================================
CREATE TABLE IF NOT EXISTS vocabulary (
    vocab_id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    term             VARCHAR(255) UNIQUE,
    meaning          VARCHAR(255) NOT NULL,
    pronunciation    VARCHAR(255),
    example          TEXT,
    example_meaning  TEXT,
    audio_path       VARCHAR(255),
    jlpt_level       VARCHAR(10)  NOT NULL,
    topic_id         UUID         NOT NULL REFERENCES topics(topic_id),
    created_at       TIMESTAMP
);

-- =============================================
-- FLASHCARDS
-- =============================================
CREATE TABLE IF NOT EXISTS flashcards (
    flashcard_id    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users(user_id),
    vocabulary_id   UUID          REFERENCES vocabulary(vocab_id),
    front_text      TEXT          NOT NULL,
    back_text       TEXT          NOT NULL,
    difficulty      DOUBLE PRECISION,
    stability       DOUBLE PRECISION,
    state           INT           NOT NULL DEFAULT 0,
    elapsed_days    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    scheduled_days  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    due             TIMESTAMP     NOT NULL DEFAULT NOW(),
    reps            INT           NOT NULL DEFAULT 0,
    lapses          INT           NOT NULL DEFAULT 0,
    is_suspended    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- =============================================
-- REVIEW LOGS
-- =============================================
CREATE TABLE IF NOT EXISTS review_logs (
    review_log_id    UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    flashcard_id     UUID             NOT NULL REFERENCES flashcards(flashcard_id),
    user_id          UUID             NOT NULL,
    rating           INT              NOT NULL,
    scheduled_days   DOUBLE PRECISION NOT NULL,
    elapsed_days     DOUBLE PRECISION NOT NULL,
    review_timestamp TIMESTAMP        NOT NULL DEFAULT NOW(),
    state            INT              NOT NULL,
    created_at       TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- =============================================
-- SAVED VOCABULARY (many-to-many: users <-> vocabulary)
-- =============================================
CREATE TABLE IF NOT EXISTS saved_vocabulary (
    vocab_id UUID NOT NULL REFERENCES vocabulary(vocab_id),
    user_id  UUID NOT NULL REFERENCES users(user_id),
    PRIMARY KEY (vocab_id, user_id)
);

-- =============================================
-- CONVERSATIONS
-- =============================================
CREATE TABLE IF NOT EXISTS conversations (
    conv_id     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    jlpt_level  VARCHAR(10),
    unit        INT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================
-- CONVERSATION LINES
-- =============================================
CREATE TABLE IF NOT EXISTS conversation_lines (
    line_id                 UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id         UUID    NOT NULL REFERENCES conversations(conv_id),
    speaker                 VARCHAR(100) NOT NULL,
    japanese_text           TEXT    NOT NULL,
    vietnamese_translation  TEXT,
    notes                   TEXT,
    important_vocab         TEXT,
    order_index             INT     NOT NULL
);

-- =============================================
-- NOTIFICATIONS
-- =============================================
CREATE TABLE IF NOT EXISTS notifications (
    notification_id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL REFERENCES users(user_id),
    title                VARCHAR(255) NOT NULL,
    message              TEXT         NOT NULL,
    type                 VARCHAR(30)  NOT NULL,
    is_read              BOOLEAN      NOT NULL DEFAULT FALSE,
    action_url           VARCHAR(255),
    notification_channel VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    sent_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    read_at              TIMESTAMP,
    review_count         INT,
    review_category      VARCHAR(255),
    priority_level       INT          NOT NULL DEFAULT 0,
    scheduled_for        TIMESTAMP,
    external_id          VARCHAR(255)
);

-- =============================================
-- FEEDBACK
-- =============================================
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(user_id),
    content_type VARCHAR(50)  NOT NULL,
    content      TEXT,
    content_id   UUID         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
