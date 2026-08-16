DELETE FROM projects
WHERE created_by_id IN (1, 2, 3)
   OR project_manager_id IN (1, 2, 3);
