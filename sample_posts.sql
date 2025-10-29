-- Insert sample medical posts for the social feed
-- Make sure the healthcare_provider with id 5 (osama) exists first

-- Insert sample medical posts
INSERT INTO medical_post (provider_id, content, privacy, created_at) VALUES
(5, 'Important reminder: Regular check-ups are essential for maintaining good health. Schedule your annual physical today! Remember to stay hydrated and maintain a balanced diet.', 'PUBLIC', NOW() - INTERVAL '2 hours'),
(5, 'New research shows that 30 minutes of daily exercise can significantly reduce the risk of heart disease. Stay active, stay healthy! Even a simple walk can make a difference.', 'PUBLIC', NOW() - INTERVAL '5 hours'),
(5, 'Mental health is just as important as physical health. If you''re feeling overwhelmed, don''t hesitate to reach out to a mental health professional. Your well-being matters.', 'PUBLIC', NOW() - INTERVAL '1 day'),
(5, 'Winter is here! Make sure to get your flu vaccine and maintain good hygiene practices to stay healthy during the cold season.', 'PUBLIC', NOW() - INTERVAL '2 days'),
(5, 'Sleep is crucial for your immune system. Aim for 7-9 hours of quality sleep each night. Your body will thank you!', 'PUBLIC', NOW() - INTERVAL '3 days');

-- You can also add some sample likes and comments if needed
-- INSERT INTO medical_post_like (post_id, like_giver_id, created_at) VALUES
-- (1, 5, NOW() - INTERVAL '1 hour'),
-- (2, 5, NOW() - INTERVAL '3 hours');

-- INSERT INTO medical_post_comment (post_id, commenter_id, content, created_at) VALUES
-- (1, 5, 'Great advice! I always remind my patients about this too.', NOW() - INTERVAL '30 minutes'),
-- (2, 5, 'Absolutely agree! Exercise is medicine.', NOW() - INTERVAL '2 hours');
