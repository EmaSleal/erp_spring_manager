-- ============================================================================
-- V21: Dedicated FACTURA_ANULAR permission (ADR-008)
-- ============================================================================
-- POST /facturas/anular/{id} and the "Anular" action in facturas.html are
-- protected by FACTURA_ANULAR instead of FACTURA_ELIMINAR.
-- 1. Ensure the FACTURA_ANULAR permission exists (dynamic permission schema:
--    permiso / rol_permiso), mirroring the Permiso enum entry.
-- 2. Grant it to every role that currently holds FACTURA_ELIMINAR.
-- INSERT IGNORE on both tables: an existing permission (codigo is unique) or
-- role assignment (PK id_rol + id_permiso) is kept untouched, so re-running is
-- safe and manual configuration is preserved.
-- ============================================================================

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('FACTURA_ANULAR', 'Anular facturas', 'Anular facturas pagadas/completadas', 'Facturación',
     true, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT rp.id_rol, anular.id_permiso
    FROM rol_permiso rp
    JOIN permiso eliminar ON eliminar.id_permiso = rp.id_permiso AND eliminar.codigo = 'FACTURA_ELIMINAR'
    JOIN permiso anular   ON anular.codigo = 'FACTURA_ANULAR';
