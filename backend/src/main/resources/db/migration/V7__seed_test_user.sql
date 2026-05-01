INSERT INTO users (sub, username, display_name, email, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000099',
    'testuser',
    'Test User',
    'test@example.com',
    now(),
    now()
)
ON CONFLICT (email) DO NOTHING;
