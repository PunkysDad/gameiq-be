-- Create social_shares table
-- This tracks when users share their quiz results to social platforms

CREATE TABLE IF NOT EXISTS social_shares (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quiz_result_id BIGINT NOT NULL REFERENCES quiz_results(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL CHECK (platform IN ('facebook', 'tiktok', 'instagram', 'twitter')),
    shared_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    share_data JSONB, -- Additional data about the share (post ID, engagement metrics, etc.)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indices for efficient querying
CREATE INDEX idx_social_shares_user_id ON social_shares(user_id);
CREATE INDEX idx_social_shares_quiz_result_id ON social_shares(quiz_result_id);
CREATE INDEX idx_social_shares_platform ON social_shares(platform);
CREATE INDEX idx_social_shares_shared_at ON social_shares(shared_at);
CREATE INDEX idx_social_shares_platform_date ON social_shares(platform, shared_at);

-- Add update trigger for updated_at
CREATE TRIGGER update_social_shares_updated_at
    BEFORE UPDATE ON social_shares
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();