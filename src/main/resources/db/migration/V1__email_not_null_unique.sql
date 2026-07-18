-- ============================================================
-- Migration: email NOT NULL + UNIQUE constraint on usuario
-- ============================================================
-- PRE-CONDITION: DataInitializer.backfillEmptyEmails() MUST have run
-- before this migration is applied. That method assigns
-- user_<telefono>@placeholder.local to every user with a null/empty email.
--
-- This file is provided as a reference migration script.
-- The project uses hibernate.ddl-auto=update (no Flyway active).
-- To apply manually, run against the target database AFTER application
-- startup (which triggers the backfill in DataInitializer.run()).
--
-- NOTE: If Flyway is added in the future, rename this file following
-- Flyway convention: V<next_version>__email_not_null_unique.sql
-- and ensure it runs AFTER the ApplicationRunner backfill completes.
-- ============================================================

-- Step 1: Verify no null emails remain (safety check — should be 0)
-- SELECT COUNT(*) FROM usuario WHERE email IS NULL OR email = '';

-- Step 2: Add NOT NULL constraint (requires all rows to have email)
ALTER TABLE usuario MODIFY COLUMN email VARCHAR(100) NOT NULL;

-- Step 3: Add UNIQUE constraint if not already present
-- (Hibernate ddl-auto=update may have already added the unique index from @Column(unique=true))
-- Run only if the unique index does not exist:
-- ALTER TABLE usuario ADD CONSTRAINT uq_usuario_email UNIQUE (email);
