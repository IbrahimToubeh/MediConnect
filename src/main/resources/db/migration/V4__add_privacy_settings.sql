-- Create user_privacy_settings table
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    profile_visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    show_email BOOLEAN NOT NULL DEFAULT FALSE,
    show_phone BOOLEAN NOT NULL DEFAULT FALSE,
    show_address BOOLEAN NOT NULL DEFAULT FALSE,
    show_medical_history BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_privacy_settings_user_id ON user_privacy_settings(user_id);

-- Add comments for documentation
COMMENT ON TABLE user_privacy_settings IS 'Stores user privacy settings';
COMMENT ON COLUMN user_privacy_settings.user_id IS 'Reference to the user these settings belong to';
COMMENT ON COLUMN user_privacy_settings.profile_visibility IS 'Profile visibility: public or private';
COMMENT ON COLUMN user_privacy_settings.show_email IS 'Whether to show email on public profile';
COMMENT ON COLUMN user_privacy_settings.show_phone IS 'Whether to show phone number on public profile';
COMMENT ON COLUMN user_privacy_settings.show_address IS 'Whether to show address on public profile';
COMMENT ON COLUMN user_privacy_settings.show_medical_history IS 'Whether to show medical history (patients only)';
