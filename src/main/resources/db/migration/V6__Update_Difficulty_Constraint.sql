-- Flyway Migration: Update difficulty level constraint to accept uppercase values
-- Version: V6__Update_Difficulty_Constraint.sql
-- Description: Updates workout_plans difficulty_level constraint to accept enum-style uppercase values

-- Drop the existing constraint
ALTER TABLE workout_plans DROP CONSTRAINT IF EXISTS workout_plans_difficulty_check;

-- Add new constraint that accepts uppercase enum values
ALTER TABLE workout_plans ADD CONSTRAINT workout_plans_difficulty_check 
    CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ELITE'));

-- Also update sport constraint to match enum format for consistency
ALTER TABLE workout_plans DROP CONSTRAINT IF EXISTS workout_plans_sport_check;
ALTER TABLE workout_plans ADD CONSTRAINT workout_plans_sport_check 
    CHECK (sport IN ('FOOTBALL', 'BASKETBALL', 'BASEBALL', 'SOCCER', 'HOCKEY'));