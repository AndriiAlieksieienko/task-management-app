DELETE FROM users
WHERE email IN (
    'user-test-admin@gmail.com',
    'user-test-manager@gmail.com',
    'user-test-manager-2@gmail.com',
    'user-test-member@gmail.com',
    'user-test-member-2@gmail.com',
    'user-test-target@gmail.com'
);