-- Functional Test Seed Data
-- This data is loaded AFTER V1 (schema) and V2 (main seed data)
-- Provides known test data for functional tests to assert against

-- Clear any existing data from V2
DELETE FROM tasks;

-- Expendable tasks (IDs 100-104) - can be modified/deleted by tests
INSERT INTO tasks (id, title, description, status, due_date, created_at, updated_at)
VALUES
  (100, 'Deletable - Pending', 'Will be deleted by test', 'PENDING', '2026-01-15 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (101, 'Deletable - In Progress', 'Will be deleted by test', 'IN_PROGRESS', '2026-01-16 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (102, 'Deletable - Completed', 'Used in delete test', 'COMPLETED', '2026-01-17 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (103, 'Modifiable - Pending', 'Used in update test', 'PENDING', '2026-01-18 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (104, 'Modifiable - No Description', NULL, 'PENDING', '2026-01-19 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00');

-- Protected tasks (IDs 105-119) - never modified, used for read-only tests
-- Mix of statuses and due dates for filtering and sorting tests
INSERT INTO tasks (id, title, description, status, due_date, created_at, updated_at)
VALUES
  (105, 'Protected - Pending 1', 'Safe task for filtering', 'PENDING', '2026-01-20 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (106, 'Protected - Pending 2', 'Safe task for filtering', 'PENDING', '2026-01-21 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (107, 'Protected - Pending 3', 'Safe task for filtering', 'PENDING', '2026-01-22 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (108, 'Protected - Pending 4', 'Safe task for filtering', 'PENDING', '2026-01-23 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (109, 'Protected - Pending 5', 'Safe task for filtering', 'PENDING', '2026-01-24 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (110, 'Protected - In Progress 1', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-25 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (111, 'Protected - In Progress 2', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-26 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (112, 'Protected - In Progress 3', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-27 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (113, 'Protected - In Progress 4', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-28 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (114, 'Protected - In Progress 5', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-29 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (115, 'Protected - Completed 1', 'Safe task for filtering', 'COMPLETED', '2026-01-30 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (116, 'Protected - Completed 2', 'Safe task for filtering', 'COMPLETED', '2026-01-31 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (117, 'Protected - Completed 3', 'Safe task for filtering', 'COMPLETED', '2026-02-01 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (118, 'Protected - Completed 4', 'Safe task for filtering', 'COMPLETED', '2026-02-02 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (119, 'Protected - Completed 5', 'Safe task for filtering', 'COMPLETED', '2026-02-03 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00');

-- Reset sequence to avoid ID conflicts with test-created tasks
-- Set next value to 200 (well above our seed data IDs 100-119)
ALTER TABLE tasks ALTER COLUMN id RESTART WITH 200;