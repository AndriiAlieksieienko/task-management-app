INSERT INTO tasks (id, project_id, assignee_id, created_by, name, description,
                    priority, status, due_date, created_at, updated_at)
VALUES (1, 1, 3, 1, 'Task One', 'First task for comment repository tests',
        'MEDIUM', 'NOT_STARTED', '2026-03-01', NOW(), NOW());

INSERT INTO tasks (id, project_id, assignee_id, created_by, name, description,
                    priority, status, due_date, created_at, updated_at)
VALUES (2, 1, 3, 1, 'Task Two', 'Second task for comment repository tests',
        'LOW', 'NOT_STARTED', '2026-03-15', NOW(), NOW());
