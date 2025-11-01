-- Users table 
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    subscription_tier VARCHAR(20) NOT NULL DEFAULT 'basic', -- basic, pro, elite
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sports and positions for MVP
CREATE TABLE sports_positions (
    id SERIAL PRIMARY KEY,
    sport VARCHAR(20) NOT NULL,
    position_code VARCHAR(10) NOT NULL,
    position_name VARCHAR(50) NOT NULL,
    position_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sport, position_code)
);

-- Predefined game IQ scenarios per sport-position
CREATE TABLE gameiq_scenarios (
    id SERIAL PRIMARY KEY,
    sport_position_id INTEGER NOT NULL REFERENCES sports_positions(id),
    scenario_title VARCHAR(100) NOT NULL,
    scenario_description TEXT NOT NULL,
    difficulty_level INTEGER DEFAULT 1, -- 1=high school, 2=college, 3=advanced
    competency_focus VARCHAR(50) NOT NULL, -- what this tests: "formation_reads", "coverage_recognition", etc.
    sample_questions TEXT, -- JSON string instead of array for better compatibility
    success_criteria TEXT, -- what demonstrates mastery
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI Conversations - each coaching session
CREATE TABLE ai_conversations (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    gameiq_scenario_id INTEGER NOT NULL REFERENCES gameiq_scenarios(id),
    sport VARCHAR(20) NOT NULL,
    position_code VARCHAR(10) NOT NULL,
    scenario_title VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'active', -- active, completed, assessed
    assessment_score DECIMAL(4,2), -- 0.00 to 100.00
    competency_demonstrated BOOLEAN DEFAULT FALSE,
    is_recruiter_visible BOOLEAN DEFAULT FALSE,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Individual messages within conversations
CREATE TABLE conversation_messages (
    id SERIAL PRIMARY KEY,
    conversation_id INTEGER NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL, -- 'user' or 'assistant'
    content TEXT NOT NULL,
    message_order INTEGER NOT NULL,
    tokens_used INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Usage tracking for subscription limits
CREATE TABLE api_usage (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    conversation_id INTEGER REFERENCES ai_conversations(id),
    tokens_used INTEGER NOT NULL,
    cost_estimate DECIMAL(6,4),
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_conversations_recruiter_visible ON ai_conversations(is_recruiter_visible);
CREATE INDEX idx_messages_conversation_id ON conversation_messages(conversation_id);
CREATE INDEX idx_scenarios_sport_position ON gameiq_scenarios(sport_position_id);
CREATE INDEX idx_usage_user_date ON api_usage(user_id, usage_date);

-- INSERT SPORTS AND POSITIONS
INSERT INTO sports_positions (sport, position_code, position_name, position_description) VALUES
-- Football
('football', 'QB', 'Quarterback', 'Field general responsible for reading defenses and making pre-snap adjustments'),
('football', 'RB', 'Running Back', 'Versatile player who must read blocking schemes and pass protections'),
('football', 'WR', 'Wide Receiver', 'Route runner who must adjust based on defensive coverage'),
('football', 'TE', 'Tight End', 'Hybrid player who blocks and receives based on formation and coverage'),
('football', 'OL', 'Offensive Line', 'Protector who must identify defensive fronts and blitzes'),
('football', 'LB', 'Linebacker', 'Defensive playmaker who reads offensive formations and covers multiple responsibilities'),
('football', 'DB', 'Defensive Back', 'Coverage specialist who must identify routes and provide help coverage'),
('football', 'DL', 'Defensive Line', 'Pass rusher who reads blocking schemes and offensive tendencies');

INSERT INTO sports_positions (sport, position_code, position_name, position_description) VALUES
-- Basketball  
('basketball', 'PG', 'Point Guard', 'Floor general who reads defenses and initiates offense'),
('basketball', 'SG', 'Shooting Guard', 'Scorer who must read defensive rotations and create shots'),
('basketball', 'SF', 'Small Forward', 'Versatile player who adapts to multiple defensive schemes'),
('basketball', 'PF', 'Power Forward', 'Interior player who reads post coverage and help defense'),
('basketball', 'C', 'Center', 'Rim protector who anchors defense and understands positioning');

INSERT INTO sports_positions (sport, position_code, position_name, position_description) VALUES
-- Baseball
('baseball', 'P', 'Pitcher', 'Game controller who sequences pitches based on count and situation'),
('baseball', 'C', 'Catcher', 'Field general who calls pitches and manages the game'),
('baseball', 'IF', 'Infielder', 'Defensive player who positions based on count, hitter, and situation'),
('baseball', 'OF', 'Outfielder', 'Coverage player who reads swings and positions for optimal defense');

INSERT INTO sports_positions (sport, position_code, position_name, position_description) VALUES
-- Soccer
('soccer', 'GK', 'Goalkeeper', 'Last line of defense who reads attacks and organizes the defense'),
('soccer', 'DEF', 'Defender', 'Defensive player who reads attacking patterns and provides coverage'),
('soccer', 'MID', 'Midfielder', 'Transition player who reads the game and controls tempo'),
('soccer', 'FWD', 'Forward', 'Attacking player who reads defensive schemes to create scoring chances');

INSERT INTO sports_positions (sport, position_code, position_name, position_description) VALUES
-- Hockey
('hockey', 'G', 'Goaltender', 'Net guardian who reads plays and anticipates shots'),
('hockey', 'D', 'Defenseman', 'Defensive player who reads the rush and manages gaps'),
('hockey', 'LW', 'Left Wing', 'Attacking player who reads defensive coverage'),
('hockey', 'C', 'Center', 'Playmaker who reads the ice and creates opportunities'),
('hockey', 'RW', 'Right Wing', 'Finisher who finds space in defensive schemes');

-- INSERT GAME IQ SCENARIOS (simplified sample questions)
-- First, we need to get the sports_positions IDs, so let's insert scenarios one by one

-- Football QB scenarios
INSERT INTO gameiq_scenarios (sport_position_id, scenario_title, scenario_description, difficulty_level, competency_focus, sample_questions, success_criteria)
SELECT sp.id, 'Pre-Snap Defensive Reads', 'Reading defensive alignment and making protection calls before the snap', 2, 'formation_recognition', 
'["What do you see when the safety rotates down late?", "How do you identify a potential blitz?", "What protection would you call against this front?"]',
'Correctly identifies defensive strengths/weaknesses and makes appropriate adjustments'
FROM sports_positions sp WHERE sp.sport='football' AND sp.position_code='QB';

INSERT INTO gameiq_scenarios (sport_position_id, scenario_title, scenario_description, difficulty_level, competency_focus, sample_questions, success_criteria)
SELECT sp.id, 'RPO Decision Making', 'Reading the conflict defender in Run-Pass Option plays', 3, 'coverage_recognition',
'["Which defender do you read on this RPO?", "When do you pull the ball vs hand it off?", "How does the safety position affect your read?"]',
'Demonstrates understanding of RPO concepts and proper read progression'
FROM sports_positions sp WHERE sp.sport='football' AND sp.position_code='QB';

-- Football WR scenarios
INSERT INTO gameiq_scenarios (sport_position_id, scenario_title, scenario_description, difficulty_level, competency_focus, sample_questions, success_criteria)
SELECT sp.id, 'Route Adjustments vs Coverage', 'Modifying routes based on defensive coverage recognition', 2, 'coverage_recognition',
'["How would you adjust this route against Cover 2?", "Where do you sit against zone coverage?", "What if you see the safety cheating over?"]',
'Demonstrates ability to find soft spots and adjust routes intelligently'
FROM sports_positions sp WHERE sp.sport='football' AND sp.position_code='WR';

-- Basketball PG scenarios  
INSERT INTO gameiq_scenarios (sport_position_id, scenario_title, scenario_description, difficulty_level, competency_focus, sample_questions, success_criteria)
SELECT sp.id, 'Zone Attack Strategies', 'Reading zone defenses and finding weaknesses', 2, 'defensive_recognition',
'["How do you attack a 2-3 zone?", "Where are the gaps in a 1-3-1?", "What movement creates the best shots against zone?"]',
'Identifies zone weaknesses and demonstrates proper attack strategies'
FROM sports_positions sp WHERE sp.sport='basketball' AND sp.position_code='PG';

INSERT INTO gameiq_scenarios (sport_position_id, scenario_title, scenario_description, difficulty_level, competency_focus, sample_questions, success_criteria)
SELECT sp.id, 'Pick and Roll Reads', 'Making correct decisions based on defensive reactions to screens', 3, 'coverage_recognition',
'["What do you do if they switch the screen?", "How do you read a hedge defense?", "When should you reject the screen?"]',
'Shows understanding of screen reactions and proper counter-moves'
FROM sports_positions sp WHERE sp.sport='basketball' AND sp.position_code='PG';

-- Baseball P scenarios
INSERT INTO gameiq_scenarios (sport_position_id, scenario_title, scenario_description, difficulty_level, competency_focus, sample_questions, success_criteria)
SELECT sp.id, 'Count Strategy', 'Understanding how to attack hitters based on ball-strike count', 2, 'situational_awareness',
'["What is your approach on 2-0?", "How do you pitch with runners in scoring position?", "What changes in a 3-1 count?"]',
'Shows understanding of count leverage and situation-based adjustments'
FROM sports_positions sp WHERE sp.sport='baseball' AND sp.position_code='P';

-- Mock user for development
INSERT INTO users (email, display_name, subscription_tier) VALUES
('test@athlete.app', 'Test Athlete', 'pro');