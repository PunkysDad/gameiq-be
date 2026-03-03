-- V8__Add_id_to_junction_tables.sql
-- Adds a surrogate PK (id) to junction tables so JPA @Entity can manage them.
-- The original composite PK unique constraint is preserved as a unique index.

-- conversation_tags
ALTER TABLE conversation_tags DROP CONSTRAINT IF EXISTS conversation_tags_pkey;
ALTER TABLE conversation_tags ADD COLUMN IF NOT EXISTS id BIGSERIAL PRIMARY KEY;
CREATE UNIQUE INDEX IF NOT EXISTS uq_conversation_tags
    ON conversation_tags (conversation_id, tag_id);

-- Update FK to point at claude_conversations (the real conversation table)
ALTER TABLE conversation_tags
    DROP CONSTRAINT IF EXISTS fk_conversation_tags_conversation_id;
ALTER TABLE conversation_tags
    ADD CONSTRAINT fk_conversation_tags_conversation_id
    FOREIGN KEY (conversation_id) REFERENCES claude_conversations(id) ON DELETE CASCADE;

-- workout_plan_tags
ALTER TABLE workout_plan_tags DROP CONSTRAINT IF EXISTS workout_plan_tags_pkey;
ALTER TABLE workout_plan_tags ADD COLUMN IF NOT EXISTS id BIGSERIAL PRIMARY KEY;
CREATE UNIQUE INDEX IF NOT EXISTS uq_workout_plan_tags
    ON workout_plan_tags (workout_plan_id, tag_id);