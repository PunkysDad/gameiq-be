-- Create quiz_results table
-- This stores individual question attempts by users

CREATE TABLE quiz_results (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100), -- Score for this individual question (0-100)
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    time_taken INTEGER, -- Time taken in seconds (optional)
    answer_selected VARCHAR(10) NOT NULL, -- The answer they selected (A, B, C, D)
    is_correct BOOLEAN NOT NULL, -- Whether they got it right
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indices for efficient querying
CREATE INDEX idx_quiz_results_user_id ON quiz_results(user_id);
CREATE INDEX idx_quiz_results_question_id ON quiz_results(question_id);
CREATE INDEX idx_quiz_results_completed_at ON quiz_results(completed_at);
CREATE INDEX idx_quiz_results_user_score ON quiz_results(user_id, score DESC);
CREATE INDEX idx_quiz_results_user_correct ON quiz_results(user_id, is_correct);

-- Add update trigger for updated_at
CREATE TRIGGER update_quiz_results_updated_at
    BEFORE UPDATE ON quiz_results
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();