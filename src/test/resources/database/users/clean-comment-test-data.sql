DELETE FROM comments;

DELETE FROM task_labels;

DELETE FROM tasks;

DELETE FROM project_members;

DELETE FROM projects;

DELETE FROM users
WHERE email IN (
    'comment-test-admin@gmail.com',
    'comment-test-manager@gmail.com',
    'comment-test-member@gmail.com',
    'comment-test-member-2@gmail.com'
);
