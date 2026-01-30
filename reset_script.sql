-- 42Cabinet Beta Reset Script (FULL WIPE VERSION)
-- ⚠️ WARNING: This will DELETE ALL USERS. Everyone must sign up again.
-- ⚠️ WARNING: You MUST execute 'redis-cli FLUSHDB' manually after this script.

-- 1. Disable FK Checks for Safety
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Wipe All History Tables
TRUNCATE TABLE lent_history;
TRUNCATE TABLE item_history;
TRUNCATE TABLE coin_history;
-- TRUNCATE TABLE alarm_event; -- Uncomment if you want to wipe alarms

-- 3. Reset Cabinet Status (Make all available)
UPDATE cabinet 
SET status = 'AVAILABLE', 
    status_note = NULL, 
    title = NULL;

-- 4. Delete All Users (Enable Re-Welcome Gift)
-- This ensures that when they join again, 'giveWelcomeGift' logic fires.
TRUNCATE TABLE user;

-- 5. Re-enable FK Checks
SET FOREIGN_KEY_CHECKS = 1;

-- Reminder
-- Execute: redis-cli FLUSHDB
