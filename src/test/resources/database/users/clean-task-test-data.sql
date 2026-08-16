DELETE FROM task_labels;

DELETE FROM tasks;

DELETE FROM project_members;

DELETE FROM projects;

DELETE FROM users
WHERE email IN (
    'task-test-admin@gmail.com',
    'task-test-manager@gmail.com',
    'task-test-member@gmail.com',
    'task-test-member-2@gmail.com'
);
