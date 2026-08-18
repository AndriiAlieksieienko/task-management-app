INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('userTestAdmin',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'user-test-admin@gmail.com', 'Admin', 'Test',
        (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'), NOW(), NOW());

INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('userTestManager',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'user-test-manager@gmail.com', 'Project', 'Manager',
        (SELECT id FROM roles WHERE name = 'ROLE_PROJECT_MANAGER'), NOW(), NOW());

INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('userTestMember',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'user-test-member@gmail.com', 'Team', 'Member',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());

INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('userTestTarget',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'user-test-target@gmail.com', 'Target', 'User',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());
