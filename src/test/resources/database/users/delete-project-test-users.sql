DELETE FROM users
WHERE email IN (
    'project-test-admin@gmail.com',
    'project-test-manager@gmail.com',
    'project-test-member@gmail.com',
    'project-test-member-2@gmail.com'
);
