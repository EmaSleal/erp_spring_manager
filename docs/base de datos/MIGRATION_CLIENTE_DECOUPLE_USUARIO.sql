-- Migration: Decouple Cliente from Usuario
-- Change: cliente-decouple-usuario
-- Date: 2026-06-16
-- IMPORTANT: Run SHOW CREATE TABLE cliente first to verify FK/index names before Steps 5-7

-- Step 1: Add new columns (safe to run even if ddl-auto=update already added them)
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS telefono VARCHAR(20) NULL;
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS direccion VARCHAR(255) NULL;

-- Step 2: Verify phone length fits before copy (run manually, abort if > 20)
-- SELECT MAX(CHAR_LENGTH(telefono)) FROM usuario;

-- Step 3: Copy existing phone data from usuario (preserves R1 data)
UPDATE cliente c
    JOIN usuario u ON c.id_usuario = u.telefono
SET c.telefono = u.telefono
WHERE c.telefono IS NULL;

-- Step 4: Validate zero data loss (must return 0 before proceeding)
-- SELECT COUNT(*) FROM cliente WHERE id_usuario IS NOT NULL AND telefono IS NULL;

-- Step 5: Drop FK constraint (verify name via SHOW CREATE TABLE cliente)
ALTER TABLE cliente DROP FOREIGN KEY FKetx0tojxf5yevxcyt6qb526x5;

-- Step 6: Drop FK index
ALTER TABLE cliente DROP INDEX FKetx0tojxf5yevxcyt6qb526x5;

-- Step 7: Drop the column
ALTER TABLE cliente DROP COLUMN id_usuario;
