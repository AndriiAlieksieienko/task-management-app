INSERT INTO projects (
    name,
    description,
    status,
    start_date,
    end_date,
    created_by_id,
    project_manager_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'Project One',
    'First project',
    'INITIATED',
    '2026-08-01',
    '2026-12-31',
    creator.id,
    manager.id,
    NOW(),
    NOW(),
    false
FROM users creator
JOIN users manager
WHERE creator.email = 'project-test-admin@gmail.com'
  AND manager.email = 'project-test-manager@gmail.com';

INSERT INTO projects (
    name,
    description,
    status,
    start_date,
    end_date,
    created_by_id,
    project_manager_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'Project Two',
    'Second project',
    'INITIATED',
    '2026-08-01',
    '2026-12-31',
    creator.id,
    manager.id,
    NOW(),
    NOW(),
    false
FROM users creator
JOIN users manager
WHERE creator.email = 'project-test-admin@gmail.com'
  AND manager.email = 'project-test-manager@gmail.com';

INSERT INTO projects (
    name,
    description,
    status,
    start_date,
    end_date,
    created_by_id,
    project_manager_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    'Project Three',
    'Third project',
    'INITIATED',
    '2026-08-01',
    '2026-12-31',
    creator.id,
    manager.id,
    NOW(),
    NOW(),
    false
FROM users creator
JOIN users manager
WHERE creator.email = 'project-test-admin@gmail.com'
  AND manager.email = 'project-test-manager@gmail.com';