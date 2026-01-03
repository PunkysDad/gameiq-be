-- V7: Update QuizResult table to match current entity structure
-- This addresses the "No property 'difficultyLevel' found for type 'QuizResult'" error
-- and adds all missing columns needed by the QuizResult entity

-- Add missing columns to match the QuizResult entity
ALTER TABLE quiz_results 
ADD COLUMN IF NOT EXISTS sport VARCHAR(50),
ADD COLUMN IF NOT EXISTS position VARCHAR(50),
ADD COLUMN IF NOT EXISTS quiz_type VARCHAR(50) NOT NULL DEFAULT 'FORMATION_RECOGNITION',
ADD COLUMN IF NOT EXISTS quiz_title VARCHAR(255) NOT NULL DEFAULT 'Default Quiz',
ADD COLUMN IF NOT EXISTS questions_json TEXT NOT NULL DEFAULT '[]',
ADD COLUMN IF NOT EXISTS user_answers_json TEXT NOT NULL DEFAULT '[]',
ADD COLUMN IF NOT EXISTS correct_answers INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS total_questions INTEGER NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS score_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS time_taken_seconds INTEGER,
ADD COLUMN IF NOT EXISTS difficulty_level VARCHAR(50) NOT NULL DEFAULT 'BEGINNER',
ADD COLUMN IF NOT EXISTS generated_by_claude BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS shared_to_facebook BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS shared_to_tiktok BOOLEAN NOT NULL DEFAULT false;

-- Rename existing columns to match entity naming convention
-- score -> score (already correct)
-- time_taken -> time_taken_seconds (we added new column above, will copy data)
UPDATE quiz_results SET time_taken_seconds = time_taken WHERE time_taken IS NOT NULL;

-- Add check constraints for enum values
ALTER TABLE quiz_results 
ADD CONSTRAINT chk_sport CHECK (sport IS NULL OR sport IN ('FOOTBALL', 'BASKETBALL', 'BASEBALL', 'SOCCER', 'HOCKEY')),
ADD CONSTRAINT chk_position CHECK (position IS NULL OR position IN (
    'QB', 'RB', 'WR', 'TE', 'OL', 'DL', 'LB', 'DB', 'K', 'P', -- Football
    'PG', 'SG', 'SF', 'PF', 'C', -- Basketball  
    'PITCHER', 'CATCHER', 'FIRST_BASE', 'SECOND_BASE', 'THIRD_BASE', 'SHORTSTOP', 'LEFT_FIELD', 'CENTER_FIELD', 'RIGHT_FIELD', -- Baseball
    'GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'STRIKER', -- Soccer
    'CENTER', 'LEFT_WING', 'RIGHT_WING', 'LEFT_DEFENSE', 'RIGHT_DEFENSE', 'GOALIE' -- Hockey
)),
ADD CONSTRAINT chk_quiz_type CHECK (quiz_type IN (
    'FORMATION_RECOGNITION', 'PLAY_CALLING', 'TACTICAL_DECISION', 
    'POSITION_KNOWLEDGE', 'RULES_AND_REGULATIONS', 'GAME_SITUATION'
)),
ADD CONSTRAINT chk_difficulty_level CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
ADD CONSTRAINT chk_score_percentage CHECK (score_percentage >= 0 AND score_percentage <= 100);

-- Update existing records to calculate score_percentage from score and total_questions
UPDATE quiz_results 
SET score_percentage = (score::DECIMAL / GREATEST(total_questions, 1)) * 100,
    correct_answers = score
WHERE score_percentage = 0;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_quiz_results_sport ON quiz_results(sport);
CREATE INDEX IF NOT EXISTS idx_quiz_results_position ON quiz_results(position);
CREATE INDEX IF NOT EXISTS idx_quiz_results_quiz_type ON quiz_results(quiz_type);
CREATE INDEX IF NOT EXISTS idx_quiz_results_difficulty ON quiz_results(difficulty_level);
CREATE INDEX IF NOT EXISTS idx_quiz_results_user_sport ON quiz_results(user_id, sport);
CREATE INDEX IF NOT EXISTS idx_quiz_results_completed_at ON quiz_results(completed_at);
CREATE INDEX IF NOT EXISTS idx_quiz_results_score_percentage ON quiz_results(score_percentage);