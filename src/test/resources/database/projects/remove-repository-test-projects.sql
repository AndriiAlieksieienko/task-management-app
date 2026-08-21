DELETE FROM project_members
WHERE project_id IN (
    SELECT id
    FROM projects
    WHERE created_by_id IN (
        SELECT id
        FROM users
        WHERE email LIKE 'user-test-%@gmail.com'
    )
    OR project_manager_id IN (
        SELECT id
        FROM users
        WHERE email LIKE 'user-test-%@gmail.com'
    )
);

DELETE FROM projects
WHERE created_by_id IN (
    SELECT id
    FROM users
    WHERE email LIKE 'user-test-%@gmail.com'
)
OR project_manager_id IN (
    SELECT id
    FROM users
    WHERE email LIKE 'user-test-%@gmail.com'
);