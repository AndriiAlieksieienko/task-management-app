DELETE FROM comments;

DELETE FROM attachments;

DELETE FROM task_labels;

DELETE FROM labels;

DELETE FROM tasks;

DELETE FROM project_members;

DELETE FROM projects;

DELETE FROM users
WHERE email LIKE 'project-test-%@gmail.com';
