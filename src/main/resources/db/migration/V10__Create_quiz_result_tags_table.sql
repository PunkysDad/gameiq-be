-- Create quiz_result_tags junction table
-- This allows users to tag their quiz results for organization

CREATE TABLE IF NOT EXISTS quiz_result_tags (
    quiz_result_id BIGINT NOT NULL REFERENCES quiz_results(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key is the combination of quiz_result_id and tag_id
    PRIMARY KEY (quiz_result_id, tag_id)
);

-- Create indices for efficient querying both directions
CREATE INDEX IF NOT EXISTS idx_quiz_result_tags_quiz_result_id ON quiz_result_tags(quiz_result_id);
CREATE INDEX IF NOT EXISTS idx_quiz_result_tags_tag_id ON quiz_result_tags(tag_id);

-- Add constraint to ensure tags belong to the same user as the quiz result
-- This is enforced through the application layer since both reference user-scoped resources