-- Functional Test Seed Data
-- This data is loaded AFTER V1 (schema) and V2 (main seed data)
-- Provides known test data for functional tests to assert against

-- Clear any existing data from V2/V4
DELETE FROM tasks;

-- Protected tasks (IDs 999001-999015) - never modified, used for read-only tests
-- Mix of statuses and due dates for filtering and sorting tests
INSERT INTO tasks (id, title, description, status, due_date, created_at, updated_at)
VALUES
  (999001, 'Protected - Pending 1', 'Safe task for filtering', 'PENDING', '2026-01-20 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999002, 'Protected - Pending 2', 'Safe task for filtering', 'PENDING', '2026-01-21 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999003, 'Protected - Pending 3', 'Safe task for filtering', 'PENDING', '2026-01-22 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999004, 'Protected - Pending 4', 'Safe task for filtering', 'PENDING', '2026-01-23 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999005, 'Protected - Pending 5', 'Safe task for filtering', 'PENDING', '2026-01-24 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999006, 'Protected - In Progress 1', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-25 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999007, 'Protected - In Progress 2', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-26 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999008, 'Protected - In Progress 3', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-27 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999009, 'Protected - In Progress 4', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-28 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999010, 'Protected - In Progress 5', 'Safe task for filtering', 'IN_PROGRESS', '2026-01-29 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999011, 'Protected - Completed 1', 'Safe task for filtering', 'COMPLETED', '2026-01-30 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999012, 'Protected - Completed 2', 'Safe task for filtering', 'COMPLETED', '2026-01-31 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999013, 'Protected - Completed 3', 'Safe task for filtering', 'COMPLETED', '2026-02-01 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999014, 'Protected - Completed 4', 'Safe task for filtering', 'COMPLETED', '2026-02-02 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999015, 'Protected - Completed 5', 'Safe task for filtering', 'COMPLETED', '2026-02-03 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00');

-- Expendable tasks (IDs 999016-999020) - can be modified/deleted by tests
INSERT INTO tasks (id, title, description, status, due_date, created_at, updated_at)
VALUES
  (999016, 'Deletable - Pending', 'Will be deleted by test', 'PENDING', '2026-01-15 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999017, 'Deletable - In Progress', 'Will be deleted by test', 'IN_PROGRESS', '2026-01-16 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999018, 'Deletable - Completed', 'Used in delete test', 'COMPLETED', '2026-01-17 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999019, 'Modifiable - Pending', 'Used in update test', 'PENDING', '2026-01-18 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
  (999020, 'Modifiable - No Description', NULL, 'PENDING', '2026-01-19 10:00:00', '2026-01-01 09:00:00', '2026-01-01 09:00:00');

-- Reset sequence to avoid ID conflicts with test-created tasks
ALTER TABLE tasks ALTER COLUMN id RESTART WITH 999100;
