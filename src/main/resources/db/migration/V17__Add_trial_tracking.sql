-- Add trial usage tracking columns to users table
ALTER TABLE users
    ADD COLUMN trial_chats_used   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trial_workouts_used INTEGER NOT NULL DEFAULT 0;