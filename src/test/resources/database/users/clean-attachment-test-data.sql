DELETE FROM attachments;

DELETE FROM task_labels;

DELETE FROM tasks;

DELETE FROM project_members;

DELETE FROM projects;

DELETE FROM users
WHERE email IN (
    'attachment-test-admin@gmail.com',
    'attachment-test-manager@gmail.com',
    'attachment-test-member@gmail.com',
    'attachment-test-member-2@gmail.com'
);
