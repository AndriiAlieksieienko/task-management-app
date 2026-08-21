DELETE FROM tasks
WHERE created_by IN (1, 2, 3)
   OR assignee_id IN (1, 2, 3);
