INSERT INTO tasks (
    id,
    project_id,
    assignee_id,
    created_by,
    name,
    description,
    priority,
    status,
    due_date,
    created_at,
    updated_at
)
VALUES (
    1,
    1,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member@gmail.com'
    ),
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-admin@gmail.com'
    ),
    'Task A1',
    'First task in project A',
    'MEDIUM',
    'NOT_STARTED',
    '2026-03-01',
    NOW(),
    NOW()
);

INSERT INTO tasks (
    id,
    project_id,
    assignee_id,
    created_by,
    name,
    description,
    priority,
    status,
    due_date,
    created_at,
    updated_at
)
VALUES (
    2,
    1,
    NULL,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-admin@gmail.com'
    ),
    'Task A2',
    'Second task in project A, unassigned',
    'LOW',
    'NOT_STARTED',
    '2026-03-15',
    NOW(),
    NOW()
);

INSERT INTO tasks (
    id,
    project_id,
    assignee_id,
    created_by,
    name,
    description,
    priority,
    status,
    due_date,
    created_at,
    updated_at
)
VALUES (
    3,
    2,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member@gmail.com'
    ),
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-admin@gmail.com'
    ),
    'Task B1',
    'First task in project B',
    'HIGH',
    'IN_PROGRESS',
    '2026-04-01',
    NOW(),
    NOW()
);