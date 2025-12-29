-- Create function for automatically updating updated_at timestamps
-- This function is used by triggers to automatically set updated_at = CURRENT_TIMESTAMP

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;