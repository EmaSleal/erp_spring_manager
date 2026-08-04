-- ============================================================================
-- V15: Fix nominas audit columns — created_by / last_modified_by to INT
-- ============================================================================
-- Reason: AuditorAware<Integer> stores user IDs; V12 created these as VARCHAR.
-- ============================================================================

ALTER TABLE nominas
    MODIFY COLUMN created_by      INT NULL,
    MODIFY COLUMN last_modified_by INT NULL;
