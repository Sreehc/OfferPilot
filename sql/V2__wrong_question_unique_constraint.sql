-- Migration: Add unique constraint to wrong_question to prevent duplicate entries
-- Run this against existing databases that were created before this constraint was added.

-- First, clean up any existing duplicates (keep the most recently updated one)
DELETE w1 FROM wrong_question w1
INNER JOIN wrong_question w2
ON w1.user_id = w2.user_id AND w1.question_id = w2.question_id AND w1.id < w2.id;

-- Then add the unique index
ALTER TABLE wrong_question ADD UNIQUE INDEX uk_user_question (user_id, question_id);
