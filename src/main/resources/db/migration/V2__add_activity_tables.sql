-- Create login_sessions table
CREATE TABLE IF NOT EXISTS login_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    location VARCHAR(255),
    device VARCHAR(100),
    browser VARCHAR(100),
    login_time TIMESTAMP NOT NULL,
    logout_time TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_time TIMESTAMP,
    CONSTRAINT unique_session_token UNIQUE (session_token)
);

-- Create account_activities table
CREATE TABLE IF NOT EXISTS account_activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    ip_address VARCHAR(45),
    location VARCHAR(255),
    device VARCHAR(100),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    additional_data TEXT
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_login_sessions_user_id ON login_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_login_sessions_session_token ON login_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_login_sessions_is_active ON login_sessions(is_active);
CREATE INDEX IF NOT EXISTS idx_login_sessions_login_time ON login_sessions(login_time);

CREATE INDEX IF NOT EXISTS idx_account_activities_user_id ON account_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_account_activities_type ON account_activities(type);
CREATE INDEX IF NOT EXISTS idx_account_activities_timestamp ON account_activities(timestamp);

-- Add comments for documentation
COMMENT ON TABLE login_sessions IS 'Tracks user login sessions with device and location information';
COMMENT ON TABLE account_activities IS 'Tracks various account activities for security and audit purposes';

COMMENT ON COLUMN login_sessions.session_token IS 'Unique session token for the login session';
COMMENT ON COLUMN login_sessions.ip_address IS 'IP address from which the user logged in';
COMMENT ON COLUMN login_sessions.user_agent IS 'Browser user agent string';
COMMENT ON COLUMN login_sessions.location IS 'Geographic location detected from IP';
COMMENT ON COLUMN login_sessions.device IS 'Device type (Desktop, Mobile, Tablet)';
COMMENT ON COLUMN login_sessions.browser IS 'Browser name and version';
COMMENT ON COLUMN login_sessions.is_active IS 'Whether the session is currently active';

COMMENT ON COLUMN account_activities.type IS 'Type of activity (LOGIN, LOGOUT, PASSWORD_CHANGE, etc.)';
COMMENT ON COLUMN account_activities.description IS 'Human-readable description of the activity';
COMMENT ON COLUMN account_activities.additional_data IS 'Additional data stored as JSON string';
