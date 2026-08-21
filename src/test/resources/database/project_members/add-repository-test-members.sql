INSERT INTO project_members (
    project_id,
    user_id,
    joined_at
)
VALUES (
    1,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member@gmail.com'
    ),
    NOW()
);

INSERT INTO project_members (
    project_id,
    user_id,
    joined_at
)
VALUES (
    1,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-target@gmail.com'
    ),
    NOW()
);

INSERT INTO project_members (
    project_id,
    user_id,
    joined_at
)
VALUES (
    2,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member@gmail.com'
    ),
    NOW()
);