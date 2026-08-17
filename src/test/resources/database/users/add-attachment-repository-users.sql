INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (1, 'attachRepoAdmin',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'attach-repo-admin@gmail.com', 'Admin', 'Test',
        (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'), NOW(), NOW());

INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (2, 'attachRepoManager',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'attach-repo-manager@gmail.com', 'Project', 'Manager',
        (SELECT id FROM roles WHERE name = 'ROLE_PROJECT_MANAGER'), NOW(), NOW());

INSERT INTO users (id, username, password, email, first_name, last_name, role_id, created_at, updated_at)
VALUES (3, 'attachRepoMember',
        '$2a$10$D9U8ne3fVtjHTV3P2Fh2n.uOX7DVwzJal0kRK5xu6WQOwj1D3fUOG',
        'attach-repo-member@gmail.com', 'Team', 'Member',
        (SELECT id FROM roles WHERE name = 'ROLE_TEAM_MEMBER'), NOW(), NOW());
