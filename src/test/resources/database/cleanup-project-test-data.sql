DELETE FROM project_members
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE email IN (
        'project-test-admin@gmail.com',
        'project-test-manager@gmail.com',
        'project-test-member@gmail.com',
        'project-test-member-2@gmail.com'
    )
);

DELETE FROM projects
WHERE created_by_id IN (
    SELECT id
    FROM users
    WHERE email IN (
        'project-test-admin@gmail.com',
        'project-test-manager@gmail.com',
        'project-test-member@gmail.com',
        'project-test-member-2@gmail.com'
    )
)
OR project_manager_id IN (
    SELECT id
    FROM users
    WHERE email IN (
        'project-test-admin@gmail.com',
        'project-test-manager@gmail.com',
        'project-test-member@gmail.com',
        'project-test-member-2@gmail.com'
    )
);

DELETE FROM users
WHERE email IN (
    'project-test-admin@gmail.com',
    'project-test-manager@gmail.com',
    'project-test-member@gmail.com',
    'project-test-member-2@gmail.com'
);