DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_claude_conversations_user_id'
    ) THEN
        ALTER TABLE claude_conversations
        ADD CONSTRAINT fk_claude_conversations_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;
