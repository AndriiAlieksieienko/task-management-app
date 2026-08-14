INSERT INTO users (
    username,
    password,
    email,
    first_name,
    last_name,
    role_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'projectTestAdmin',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO4fF5n6Q8qJm8yqj8wW9p9j9J9J9J9',
    'project-test-admin@gmail.com',
    'Admin',
    'Test',
    id,
    NOW(),
    NOW(),
    false
FROM roles
WHERE name = 'ROLE_ADMIN';

INSERT INTO users (
    username,
    password,
    email,
    first_name,
    last_name,
    role_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'projectTestManager',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO4fF5n6Q8qJm8yqj8wW9p9j9J9J9J9',
    'project-test-manager@gmail.com',
    'Project',
    'Manager',
    id,
    NOW(),
    NOW(),
    false
FROM roles
WHERE name = 'ROLE_PROJECT_MANAGER';

INSERT INTO users (
    username,
    password,
    email,
    first_name,
    last_name,
    role_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'projectTestMember',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO4fF5n6Q8qJm8yqj8wW9p9j9J9J9J9',
    'project-test-member@gmail.com',
    'Team',
    'Member',
    id,
    NOW(),
    NOW(),
    false
FROM roles
WHERE name = 'ROLE_TEAM_MEMBER';

INSERT INTO users (
    username,
    password,
    email,
    first_name,
    last_name,
    role_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'projectTestMember2',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO4fF5n6Q8qJm8yqj8wW9p9j9J9J9J9',
    'project-test-member-2@gmail.com',
    'Second',
    'Member',
    id,
    NOW(),
    NOW(),
    false
FROM roles
WHERE name = 'ROLE_TEAM_MEMBER';
