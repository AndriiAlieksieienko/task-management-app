INSERT INTO project_members (
    project_id,
    user_id,
    joined_at
)
SELECT
    project.id,
    user.id,
    NOW()
FROM projects project
JOIN users user
WHERE project.name = 'Project One'
  AND user.email = 'project-test-member@gmail.com';

INSERT INTO project_members (
    project_id,
    user_id,
    joined_at
)
SELECT
    project.id,
    user.id,
    NOW()
FROM projects project
JOIN users user
WHERE project.name = 'Project Two'
  AND user.email = 'project-test-member-2@gmail.com';

INSERT INTO project_members (
    project_id,
    user_id,
    joined_at
)
SELECT
    project.id,
    user.id,
    NOW()
FROM projects project
JOIN users user
WHERE project.name = 'Project Three'
  AND user.email = 'project-test-member@gmail.com';