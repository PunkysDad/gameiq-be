-- Create quiz_questions table
-- This stores the individual quiz questions for each category

CREATE TABLE quiz_questions (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    category_id BIGINT NOT NULL REFERENCES quiz_categories(id) ON DELETE CASCADE,
    question_id VARCHAR(50) NOT NULL, -- The ID from JSON file like "pitcher_count_001"
    scenario TEXT NOT NULL, -- The scenario description
    question TEXT NOT NULL, -- The actual question
    options JSONB NOT NULL, -- Array of options with id and text
    correct VARCHAR(10) NOT NULL, -- The correct answer ID (A, B, C, D)
    explanation TEXT NOT NULL, -- Explanation of the correct answer
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('beginner', 'intermediate', 'advanced')),
    tags JSONB, -- Array of tags for this question
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique question_id per category
    UNIQUE(category_id, question_id)
);

-- Create indices for efficient querying
CREATE INDEX idx_quiz_questions_category_id ON quiz_questions(category_id);
CREATE INDEX idx_quiz_questions_difficulty ON quiz_questions(difficulty);
CREATE INDEX idx_quiz_questions_tags ON quiz_questions USING GIN(tags);

-- Add update trigger for updated_at
CREATE TRIGGER update_quiz_questions_updated_at
    BEFORE UPDATE ON quiz_questions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();