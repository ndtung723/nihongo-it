CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    preference VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, preference)
);

-- Migrate existing CSV data from users.notification_preferences column
INSERT INTO user_notification_preferences (user_id, preference)
SELECT user_id, trim(unnest(string_to_array(notification_preferences, ',')))
FROM users
WHERE notification_preferences IS NOT NULL
  AND notification_preferences != ''
ON CONFLICT DO NOTHING;

ALTER TABLE users DROP COLUMN IF EXISTS notification_preferences;
