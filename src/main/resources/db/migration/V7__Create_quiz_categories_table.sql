-- Create quiz_categories table
-- This stores the different categories of quizzes for each sport/position combination

CREATE TABLE quiz_categories (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    sport VARCHAR(50) NOT NULL,
    position VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique category names per sport/position
    UNIQUE(sport, position, category_name)
);

-- Create index for efficient querying by sport/position
CREATE INDEX idx_quiz_categories_sport_position ON quiz_categories(sport, position);

-- Add update trigger for updated_at
CREATE TRIGGER update_quiz_categories_updated_at
    BEFORE UPDATE ON quiz_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();