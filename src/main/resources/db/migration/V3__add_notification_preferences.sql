-- Create user_notification_preferences table
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    -- Email notifications
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Push notifications
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Social media notifications
    post_likes BOOLEAN NOT NULL DEFAULT TRUE,
    post_comments BOOLEAN NOT NULL DEFAULT TRUE,
    comment_replies BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Healthcare notifications
    appointment_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    prescription_updates BOOLEAN NOT NULL DEFAULT TRUE,
    lab_results BOOLEAN NOT NULL DEFAULT TRUE,
    medication_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Security notifications
    security_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    login_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- System notifications
    system_updates BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id ON user_notification_preferences(user_id);

-- Add comments for documentation
COMMENT ON TABLE user_notification_preferences IS 'Stores user notification preferences for different types of notifications';
COMMENT ON COLUMN user_notification_preferences.user_id IS 'Reference to the user these preferences belong to';
COMMENT ON COLUMN user_notification_preferences.email_notifications IS 'Whether to receive email notifications';
COMMENT ON COLUMN user_notification_preferences.push_notifications IS 'Whether to receive push notifications';
COMMENT ON COLUMN user_notification_preferences.post_likes IS 'Whether to receive notifications for post likes';
COMMENT ON COLUMN user_notification_preferences.post_comments IS 'Whether to receive notifications for post comments';
COMMENT ON COLUMN user_notification_preferences.comment_replies IS 'Whether to receive notifications for comment replies';
COMMENT ON COLUMN user_notification_preferences.appointment_reminders IS 'Whether to receive appointment reminders';
COMMENT ON COLUMN user_notification_preferences.prescription_updates IS 'Whether to receive prescription updates';
COMMENT ON COLUMN user_notification_preferences.lab_results IS 'Whether to receive lab results notifications';
COMMENT ON COLUMN user_notification_preferences.medication_reminders IS 'Whether to receive medication reminders';
COMMENT ON COLUMN user_notification_preferences.security_alerts IS 'Whether to receive security alerts';
COMMENT ON COLUMN user_notification_preferences.login_alerts IS 'Whether to receive login notifications';
COMMENT ON COLUMN user_notification_preferences.password_change_alerts IS 'Whether to receive password change notifications';
COMMENT ON COLUMN user_notification_preferences.system_updates IS 'Whether to receive system update notifications';
COMMENT ON COLUMN user_notification_preferences.maintenance_alerts IS 'Whether to receive maintenance alerts';
