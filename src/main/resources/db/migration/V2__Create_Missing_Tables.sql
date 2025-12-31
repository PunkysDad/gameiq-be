-- Flyway Migration: Create missing core tables
-- Version: V2__Create_Missing_Tables.sql
-- Description: Creates workout_plans and user_workout_preferences tables needed by the application

-- Create workout_plans table
CREATE TABLE IF NOT EXISTS workout_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sport VARCHAR(50) NOT NULL,
    position VARCHAR(100) NOT NULL,
    workout_name VARCHAR(255),
    position_focus VARCHAR(255),
    difficulty_level VARCHAR(20),
    duration_minutes INTEGER,
    equipment_needed TEXT,
    generated_content TEXT,
    is_saved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_workout_plans_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT workout_plans_sport_check 
        CHECK (sport IN ('football', 'basketball', 'baseball', 'soccer', 'hockey')),
    CONSTRAINT workout_plans_difficulty_check 
        CHECK (difficulty_level IN ('beginner', 'intermediate', 'advanced')),
    CONSTRAINT workout_plans_duration_positive 
        CHECK (duration_minutes > 0)
);

-- Create user_workout_preferences table
CREATE TABLE IF NOT EXISTS user_workout_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    experience_level VARCHAR(20),
    preferred_duration INTEGER,
    available_equipment TEXT,
    training_goals TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_user_workout_preferences_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_workout_preferences_experience_check 
        CHECK (experience_level IN ('beginner', 'intermediate', 'advanced')),
    CONSTRAINT user_workout_preferences_duration_positive 
        CHECK (preferred_duration > 0)
);

-- Create user_conversations table if it doesn't exist
CREATE TABLE IF NOT EXISTS user_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_type VARCHAR(50) NOT NULL DEFAULT 'coaching',
    conversation_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_user_conversations_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_conversations_type_check 
        CHECK (conversation_type IN ('coaching', 'quiz', 'general'))
);

-- Indexes for performance
CREATE INDEX idx_workout_plans_user_id ON workout_plans(user_id);
CREATE INDEX idx_workout_plans_sport_position ON workout_plans(sport, position);
CREATE INDEX idx_workout_plans_created_at ON workout_plans(created_at);

CREATE INDEX idx_user_workout_preferences_user_id ON user_workout_preferences(user_id);

CREATE INDEX idx_user_conversations_user_id ON user_conversations(user_id);
CREATE INDEX idx_user_conversations_type ON user_conversations(conversation_type);
CREATE INDEX idx_user_conversations_created_at ON user_conversations(created_at);

-- Update trigger for workout_plans
CREATE OR REPLACE FUNCTION update_workout_plans_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER workout_plans_updated_at_trigger
    BEFORE UPDATE ON workout_plans
    FOR EACH ROW
    EXECUTE FUNCTION update_workout_plans_updated_at();

-- Update trigger for user_workout_preferences
CREATE OR REPLACE FUNCTION update_user_workout_preferences_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER user_workout_preferences_updated_at_trigger
    BEFORE UPDATE ON user_workout_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_user_workout_preferences_updated_at();

-- Update trigger for user_conversations
CREATE OR REPLACE FUNCTION update_user_conversations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER user_conversations_updated_at_trigger
    BEFORE UPDATE ON user_conversations
    FOR EACH ROW
    EXECUTE FUNCTION update_user_conversations_updated_at();

-- Table comments
COMMENT ON TABLE workout_plans IS 'Generated workout plans for users with position-specific training';
COMMENT ON TABLE user_workout_preferences IS 'User preferences for workout generation';
COMMENT ON TABLE user_conversations IS 'User conversations with Claude AI coaching system';