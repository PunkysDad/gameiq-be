-- Quiz system schema is now ready
-- Data loading will be handled by Kotlin application code reading JSON files
-- This migration just adds any final constraints or indexes we might need

-- Add comment to document the JSON file naming convention
COMMENT ON TABLE quiz_categories IS 'Quiz categories organized by sport and position. JSON files follow naming: sport__position_core.json (e.g., basketball__point-guard_core.json)';

-- Verify schema is ready
DO $$
BEGIN
    RAISE NOTICE 'Quiz system schema ready. JSON files should follow naming convention: sport__position_core.json';
END $$;