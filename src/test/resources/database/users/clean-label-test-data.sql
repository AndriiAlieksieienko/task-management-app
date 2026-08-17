DELETE FROM task_labels;

DELETE FROM labels;

DELETE FROM tasks;

DELETE FROM project_members;

DELETE FROM projects;

DELETE FROM users
WHERE email IN (
    'label-test-admin@gmail.com',
    'label-test-manager@gmail.com',
    'label-test-member@gmail.com',
    'label-test-member-2@gmail.com'
);
