-- Update subscription_tier default from FREE to TRIAL
-- Column is varchar(50) so no enum type changes needed

ALTER TABLE users
    ALTER COLUMN subscription_tier SET DEFAULT 'TRIAL';

-- Update any existing FREE users to TRIAL (safety net)
UPDATE users SET subscription_tier = 'TRIAL' WHERE subscription_tier = 'FREE';