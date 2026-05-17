-- V4: Flashcard statistics materialized view per user
-- Used by admin statistics endpoints to efficiently aggregate flashcard data

CREATE OR REPLACE VIEW flashcard_stats_per_user AS
SELECT
    u.user_id,
    COUNT(f.flashcard_id)                                              AS total_cards,
    COUNT(f.flashcard_id) FILTER (WHERE f.due <= NOW())               AS due_today,
    COUNT(f.flashcard_id) FILTER (WHERE f.state = 0)                  AS new_cards,
    COUNT(f.flashcard_id) FILTER (WHERE f.state = 1)                  AS learning_cards,
    COUNT(f.flashcard_id) FILTER (WHERE f.state = 2)                  AS review_cards,
    COUNT(f.flashcard_id) FILTER (WHERE f.state = 3)                  AS relearning_cards,
    COALESCE(
        ROUND(
            AVG(
                CASE
                    WHEN f.stability IS NOT NULL AND f.stability > 0
                    THEN (1 + f.scheduled_days / (9.0 * f.stability)) ^ -1
                    ELSE NULL
                END
            )::NUMERIC,
            2
        ),
        0
    )                                                                  AS avg_retention
FROM users u
LEFT JOIN flashcards f ON f.user_id = u.user_id AND f.is_suspended = FALSE
GROUP BY u.user_id;

COMMENT ON VIEW flashcard_stats_per_user IS
    'Aggregated FSRS flashcard statistics per user. Queried by admin dashboard and statistics endpoints.';
