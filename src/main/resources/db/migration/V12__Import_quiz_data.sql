-- Import quiz data from JSON files
-- This migration imports the existing quiz data from the static JSON files
-- Note: This is a sample migration showing the structure. Full import should be done
-- programmatically by reading the JSON files or through a data loading script.

-- First, insert baseball pitcher categories
INSERT INTO quiz_categories (sport, position, category_name, description) VALUES
('baseball', 'pitcher', 'count-management', 'Working ahead, getting ahead 0-1, avoiding hitter''s counts'),
('baseball', 'pitcher', 'sequencing', 'Setting up batters, tunneling pitches, changing eye levels'),
('baseball', 'pitcher', 'situational-pitching', 'RISP, bases loaded, 2-out situations, and game state awareness'),
('baseball', 'pitcher', 'batter-tendencies', 'Hot zones, cold zones, situational hitting patterns, and scouting reports'),
('baseball', 'pitcher', 'game-state-awareness', 'Inning, score, base runners, and defensive positioning considerations'),
('baseball', 'pitch-selection', 'pitch-selection', 'Fastball counts vs breaking ball counts, out pitches, and situational pitch usage');

-- Sample questions from count-management category
-- (In production, this should be done via a data loading script that reads the JSON files)

-- Insert sample questions to verify schema works
WITH count_management_category AS (
    SELECT id FROM quiz_categories 
    WHERE sport = 'baseball' AND position = 'pitcher' AND category_name = 'count-management'
)
INSERT INTO quiz_questions (category_id, question_id, scenario, question, options, correct, explanation, difficulty, tags)
SELECT 
    cm.id,
    'pitcher_count_001',
    '0-0 count to a .300 hitter with runners in scoring position. You have a 4-seam fastball, slider, and changeup in your arsenal.',
    'What''s your best approach for the first pitch?',
    '[
        {"id": "A", "text": "Fastball down the middle - challenge him"},
        {"id": "B", "text": "Slider in the dirt - see if he chases"},
        {"id": "C", "text": "Fastball on the corners for a strike"},
        {"id": "D", "text": "Changeup to throw off his timing"}
    ]'::jsonb,
    'C',
    'Getting ahead 0-1 is crucial with RISP. A well-located fastball on the corners gives you the best chance for a first-pitch strike without giving him something fat to hit. Avoid the heart of the plate but don''t risk being behind in the count.',
    'intermediate',
    '["0-0-count", "risp", "first-pitch-strike", "corner-location"]'::jsonb
FROM count_management_category cm

UNION ALL

SELECT 
    cm.id,
    'pitcher_count_002',
    'You''re ahead 0-2 on a aggressive hitter known for swinging at bad pitches early in at-bats.',
    'How should you approach the 0-2 pitch?',
    '[
        {"id": "A", "text": "Challenge with your best fastball - finish him off"},
        {"id": "B", "text": "Waste a pitch off the plate to see if he chases"},
        {"id": "C", "text": "Throw a strike with your out pitch"},
        {"id": "D", "text": "Mix speeds and throw a changeup in the zone"}
    ]'::jsonb,
    'B',
    'With an aggressive hitter who chases bad pitches, you have the luxury of wasting a pitch off the plate. Make him chase your pitch rather than giving him something to hit. You''re in complete control at 0-2.',
    'beginner',
    '["0-2-count", "aggressive-hitter", "waste-pitch", "chase-pitch"]'::jsonb
FROM count_management_category cm;