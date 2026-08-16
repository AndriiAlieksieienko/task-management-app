INSERT INTO projects (id, name, description, status, start_date, end_date,
                       created_by_id, project_manager_id, created_at, updated_at)
VALUES (1, 'Project A', 'First project for task repository tests', 'INITIATED',
        '2026-01-01', '2026-06-30', 1, 2, NOW(), NOW());

INSERT INTO projects (id, name, description, status, start_date, end_date,
                       created_by_id, project_manager_id, created_at, updated_at)
VALUES (2, 'Project B', 'Second project for task repository tests', 'INITIATED',
        '2026-02-01', '2026-07-31', 1, 2, NOW(), NOW());
