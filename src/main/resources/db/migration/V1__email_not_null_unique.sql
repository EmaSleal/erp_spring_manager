-- Migration V1: email NOT NULL + UNIQUE constraint on usuario.
-- PRE-CONDITION: DataInitializer.backfillEmptyEmails() runs on startup and assigns
-- user_<telefono>@placeholder.local to any user with a null/empty email before this runs.
ALTER TABLE usuario MODIFY COLUMN email VARCHAR(100) NOT NULL;
-- ALTER TABLE usuario ADD CONSTRAINT uq_usuario_email UNIQUE (email);
-- (index may already exist from Hibernate ddl-auto=update; uncomment only if missing)
