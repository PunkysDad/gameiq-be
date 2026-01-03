-- Add quiz sessions to support identical retakes and progression tracking
-- This allows users to retake the exact same 15 questions

CREATE TABLE IF NOT EXISTS quiz_sessions (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quiz_type VARCHAR(50) NOT NULL CHECK (quiz_type IN ('CORE', 'GENERATED')), 
    sport VARCHAR(50) NOT NULL,
    position VARCHAR(50) NOT NULL,
    question_ids JSONB NOT NULL, -- Array of 15 question IDs for this quiz
    session_name VARCHAR(200) NOT NULL, -- e.g., "Core Quarterback Quiz", "Generated Quarterback Quiz #1"
    is_completed BOOLEAN DEFAULT FALSE,
    best_score INTEGER DEFAULT 0, -- Best score achieved on this quiz (0-100)
    best_attempt_id BIGINT, -- Reference to the best quiz_session_attempt
    total_attempts INTEGER DEFAULT 0,
    passed BOOLEAN DEFAULT FALSE, -- TRUE if best_score >= 70
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique naming per user
    UNIQUE(user_id, session_name)
);

-- Individual attempts at a quiz session (for retakes)
CREATE TABLE IF NOT EXISTS quiz_session_attempts (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    quiz_session_id BIGINT NOT NULL REFERENCES quiz_sessions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    total_score INTEGER NOT NULL DEFAULT 0, -- 0-100 overall score for the 15 questions
    correct_answers INTEGER NOT NULL DEFAULT 0, -- Number of correct answers (0-15)
    time_taken INTEGER, -- Total time in seconds
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique attempt numbers per session
    UNIQUE(quiz_session_id, attempt_number)
);

-- Individual question results within a session attempt
CREATE TABLE IF NOT EXISTS quiz_session_question_results (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    session_attempt_id BIGINT NOT NULL REFERENCES quiz_session_attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    question_number INTEGER NOT NULL, -- Position in the quiz (1-15)
    answer_selected VARCHAR(10) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    time_taken INTEGER, -- Time for this individual question
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique question numbers per attempt
    UNIQUE(session_attempt_id, question_number),
    -- Ensure each question only appears once per attempt
    UNIQUE(session_attempt_id, question_id)
);

-- Create indices for performance
CREATE INDEX idx_quiz_sessions_user_sport_position ON quiz_sessions(user_id, sport, position);
CREATE INDEX idx_quiz_sessions_user_passed ON quiz_sessions(user_id, passed);
CREATE INDEX idx_quiz_session_attempts_session_id ON quiz_session_attempts(quiz_session_id);
CREATE INDEX idx_quiz_session_attempts_user_completed ON quiz_session_attempts(user_id, completed_at);
CREATE INDEX idx_quiz_session_question_results_attempt ON quiz_session_question_results(session_attempt_id);

-- Add update triggers
CREATE TRIGGER update_quiz_sessions_updated_at
    BEFORE UPDATE ON quiz_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to update quiz session stats after attempt completion
CREATE OR REPLACE FUNCTION update_quiz_session_stats()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE quiz_sessions 
    SET 
        total_attempts = total_attempts + 1,
        best_score = GREATEST(best_score, NEW.total_score),
        passed = CASE WHEN GREATEST(best_score, NEW.total_score) >= 70 THEN TRUE ELSE FALSE END,
        best_attempt_id = CASE 
            WHEN NEW.total_score > best_score THEN NEW.id 
            ELSE best_attempt_id 
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.quiz_session_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update session stats when attempt is completed
CREATE TRIGGER trigger_update_quiz_session_stats
    AFTER INSERT ON quiz_session_attempts
    FOR EACH ROW
    EXECUTE FUNCTION update_quiz_session_stats();

-- Create initial core quiz sessions for existing users
-- This will be populated by the application when it detects existing users
COMMENT ON TABLE quiz_sessions IS 'Quiz sessions support identical retakes and track progression. Core quizzes must be passed with 70% before generating new quizzes.';