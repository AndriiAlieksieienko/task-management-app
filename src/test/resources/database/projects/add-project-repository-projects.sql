INSERT INTO projects (id, name, description, status, start_date, end_date,
                       created_by_id, project_manager_id, created_at, updated_at)
VALUES (1, 'Website Redesign', 'Redesign of the marketing website', 'INITIATED',
        '2026-01-01', '2026-06-30', 1, 2, NOW(), NOW());

INSERT INTO projects (id, name, description, status, start_date, end_date,
                       created_by_id, project_manager_id, created_at, updated_at)
VALUES (2, 'Mobile App Launch', 'Launch of the companion mobile app', 'IN_PROGRESS',
        '2026-03-01', '2026-09-30', 1, 2, NOW(), NOW());

INSERT INTO projects (id, name, description, status, start_date, end_date,
                       created_by_id, project_manager_id, created_at, updated_at)
VALUES (3, 'Data Migration', 'Migration to the new data warehouse', 'INITIATED',
        '2025-11-01', '2026-02-28', 1, 3, NOW(), NOW());
