INSERT INTO attachments (
    id,
    task_id,
    uploaded_by,
    dropbox_file_id,
    filename,
    upload_date
)
VALUES (
    1,
    1,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member@gmail.com'
    ),
    'dropbox-id-1',
    'design.pdf',
    NOW()
);

INSERT INTO attachments (
    id,
    task_id,
    uploaded_by,
    dropbox_file_id,
    filename,
    upload_date
)
VALUES (
    2,
    1,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member-2@gmail.com'
    ),
    'dropbox-id-2',
    'notes.txt',
    NOW()
);

INSERT INTO attachments (
    id,
    task_id,
    uploaded_by,
    dropbox_file_id,
    filename,
    upload_date
)
VALUES (
    3,
    2,
    (
        SELECT id
        FROM users
        WHERE email = 'user-test-member@gmail.com'
    ),
    'dropbox-id-3',
    'screenshot.png',
    NOW()
);