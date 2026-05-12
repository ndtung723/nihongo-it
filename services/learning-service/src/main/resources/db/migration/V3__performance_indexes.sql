-- Performance indexes for learning-service

-- Flashcards: composite index for the most common query (due cards by user)
CREATE INDEX IF NOT EXISTS idx_flashcards_user_due ON flashcards(user_id, due);
CREATE INDEX IF NOT EXISTS idx_flashcards_vocab_id ON flashcards(vocabulary_id);
CREATE INDEX IF NOT EXISTS idx_flashcards_due ON flashcards(due);

-- Review logs: composite index for streak and statistics queries
CREATE INDEX IF NOT EXISTS idx_review_logs_user_timestamp ON review_logs(user_id, review_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_review_logs_timestamp ON review_logs(review_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_review_logs_flashcard_id ON review_logs(flashcard_id);
