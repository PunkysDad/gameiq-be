-- Flyway Migration: Create tags table
-- Version: V3__Create_Tags_Table.sql
-- Description: Creates the core tags table for user-scoped tagging system

CREATE TABLE IF NOT EXISTS tags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#007AFF', -- Hex color code (e.g., #FF5733)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_tags_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT tags_name_not_empty 
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT tags_color_format 
        CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT tags_name_max_length 
        CHECK (LENGTH(name) <= 100)
);

-- Indexes for performance
CREATE INDEX idx_tags_user_id ON tags(user_id);
CREATE UNIQUE INDEX idx_tags_user_name_unique ON tags(user_id, LOWER(name));

-- Comments for documentation
COMMENT ON TABLE tags IS 'User-scoped tags for organizing conversations, workouts, and quiz results';
COMMENT ON COLUMN tags.user_id IS 'Foreign key to users table - each user has their own tag namespace';
COMMENT ON COLUMN tags.name IS 'Display name of the tag (e.g., "Leg Day", "4th Down Scenarios")';
COMMENT ON COLUMN tags.color IS 'Hex color code for visual organization in UI';

-- Update trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_tags_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER tags_updated_at_trigger
    BEFORE UPDATE ON tags
    FOR EACH ROW
    EXECUTE FUNCTION update_tags_updated_at();