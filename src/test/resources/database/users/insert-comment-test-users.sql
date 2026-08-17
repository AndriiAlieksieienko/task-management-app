INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('commentTestAdmin',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'comment-test-admin@gmail.com', 'Admin', 'Test',
        (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'), NOW(), NOW());

INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('commentTestManager',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'comment-test-manager@gmail.com', 'Project', 'Manager',
        (SELECT id FROM roles WHERE name = 'ROLE_PROJECT_MANAGER'), NOW(), NOW());

INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('commentTestMember',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'comment-test-member@gmail.com', 'Team', 'Member',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());

INSERT INTO users (username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES ('commentTestMember2',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'comment-test-member-2@gmail.com', 'Second', 'Member',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());
