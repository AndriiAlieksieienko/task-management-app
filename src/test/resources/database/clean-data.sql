DELETE FROM task_labels;

DELETE FROM comments;

DELETE FROM attachments;

DELETE FROM tasks;

DELETE FROM project_members;

DELETE FROM labels;

DELETE FROM projects;

DELETE FROM users
WHERE email LIKE 'user-test-%@gmail.com';
