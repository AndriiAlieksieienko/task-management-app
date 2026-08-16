INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (1, 'pmRepoAdmin',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'pm-repo-admin@gmail.com', 'Admin', 'Test',
        (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'), NOW(), NOW());

INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (2, 'pmRepoManager',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'pm-repo-manager@gmail.com', 'Project', 'Manager',
        (SELECT id FROM roles WHERE name = 'ROLE_PROJECT_MANAGER'), NOW(), NOW());

INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (3, 'pmRepoMember',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'pm-repo-member@gmail.com', 'Team', 'Member',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());

INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (4, 'pmRepoMember2',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'pm-repo-member-2@gmail.com', 'Second', 'Member',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());
