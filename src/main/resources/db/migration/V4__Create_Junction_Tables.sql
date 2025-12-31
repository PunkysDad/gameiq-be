-- Flyway Migration: Create junction tables for tag associations
-- Version: V4__Create_Junction_Tables.sql
-- Description: Creates junction tables for many-to-many relationships between tags and content

-- Junction table: conversation_tags
CREATE TABLE IF NOT EXISTS conversation_tags (
    conversation_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (conversation_id, tag_id),
    
    -- Foreign key constraints
    CONSTRAINT fk_conversation_tags_conversation_id 
        FOREIGN KEY (conversation_id) REFERENCES user_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_tags_tag_id 
        FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Junction table: workout_plan_tags  
CREATE TABLE IF NOT EXISTS workout_plan_tags (
    workout_plan_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (workout_plan_id, tag_id),
    
    -- Foreign key constraints
    CONSTRAINT fk_workout_plan_tags_workout_plan_id 
        FOREIGN KEY (workout_plan_id) REFERENCES workout_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_workout_plan_tags_tag_id 
        FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Junction table: quiz_result_tags (for future quiz system)
CREATE TABLE IF NOT EXISTS quiz_result_tags (
    quiz_result_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (quiz_result_id, tag_id),
    
    -- Foreign key constraints will be added when quiz_results table exists
    CONSTRAINT fk_quiz_result_tags_tag_id 
        FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
    -- Note: quiz_results FK constraint will be added in future migration
);

-- Indexes for performance
CREATE INDEX idx_conversation_tags_conversation_id ON conversation_tags(conversation_id);
CREATE INDEX idx_conversation_tags_tag_id ON conversation_tags(tag_id);

CREATE INDEX idx_workout_plan_tags_workout_plan_id ON workout_plan_tags(workout_plan_id);
CREATE INDEX idx_workout_plan_tags_tag_id ON workout_plan_tags(tag_id);

CREATE INDEX idx_quiz_result_tags_quiz_result_id ON quiz_result_tags(quiz_result_id);
CREATE INDEX idx_quiz_result_tags_tag_id ON quiz_result_tags(tag_id);

-- Table comments
COMMENT ON TABLE conversation_tags IS 'Junction table linking tags to user conversations';
COMMENT ON TABLE workout_plan_tags IS 'Junction table linking tags to workout plans';
COMMENT ON TABLE quiz_result_tags IS 'Junction table linking tags to quiz results (future)';

-- Column comments
COMMENT ON COLUMN conversation_tags.conversation_id IS 'Foreign key to user_conversations table';
COMMENT ON COLUMN conversation_tags.tag_id IS 'Foreign key to tags table';
COMMENT ON COLUMN workout_plan_tags.workout_plan_id IS 'Foreign key to workout_plans table';
COMMENT ON COLUMN workout_plan_tags.tag_id IS 'Foreign key to tags table';
COMMENT ON COLUMN quiz_result_tags.quiz_result_id IS 'Foreign key to quiz_results table (to be created)';
COMMENT ON COLUMN quiz_result_tags.tag_id IS 'Foreign key to tags table';