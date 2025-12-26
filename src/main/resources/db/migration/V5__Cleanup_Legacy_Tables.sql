-- Flyway Migration: Remove legacy UUID-based tables
-- Version: V5__Cleanup_Legacy_Tables.sql
-- Description: Removes disconnected UUID-based tables that are not used by the current Kotlin application

-- Drop tables in correct order (foreign keys first)
DROP TABLE IF EXISTS social_shares CASCADE;
DROP TABLE IF EXISTS quiz_sessions CASCADE;
DROP TABLE IF EXISTS quiz_questions CASCADE;
DROP TABLE IF EXISTS ai_conversations CASCADE;
DROP TABLE IF EXISTS athlete_profiles CASCADE;

-- Drop any unused custom types (if they exist and aren't needed)
-- Note: Only drop if no other tables use these types
DO $$ 
DECLARE
    type_name text;
BEGIN
    -- Check if sport_type enum exists and drop if unused
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sport_type') THEN
        -- Check if any tables still use this type
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE udt_name = 'sport_type'
            AND table_name NOT IN ('athlete_profiles', 'ai_conversations', 'quiz_questions', 'quiz_sessions')
        ) THEN
            DROP TYPE IF EXISTS sport_type CASCADE;
        END IF;
    END IF;
    
    -- Check if difficulty_level enum exists and drop if unused
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'difficulty_level') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE udt_name = 'difficulty_level'
            AND table_name NOT IN ('athlete_profiles', 'quiz_questions')
        ) THEN
            DROP TYPE IF EXISTS difficulty_level CASCADE;
        END IF;
    END IF;
    
    -- Check if equipment_type enum exists and drop if unused
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'equipment_type') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE udt_name = 'equipment_type'
            AND table_name NOT IN ('athlete_profiles')
        ) THEN
            DROP TYPE IF EXISTS equipment_type CASCADE;
        END IF;
    END IF;
END $$;

-- Drop any unused functions
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- Verification query - these should return 0 rows
-- SELECT table_name FROM information_schema.tables 
-- WHERE table_name IN ('athlete_profiles', 'ai_conversations', 'quiz_questions', 'quiz_sessions', 'social_shares');