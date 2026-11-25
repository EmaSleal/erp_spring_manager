-- T-056 (ADR-072): régimen tributario con vigencia. Cada fila es un tramo que rige desde vigente_desde hasta el día
-- anterior al inicio del siguiente (no hay fecha de fin ni régimen por defecto). El régimen de un documento es el del
-- tramo con el mayor vigente_desde menor o igual a la fecha del documento.
--
-- Semilla (solo el pasado y el presente; el tramo de 2027 NO se inserta):
--   * SIMPLIFICADO desde 2000-01-01: fecha anterior a cualquier documento del sistema (el dump de producción del
--     2026-11-18 tiene como primera factura el 2026-07-04; la primera nota de crédito y el primer comprobante son de
--     2026-11-13 y no hay cuentas por pagar). El titular estaba en el régimen simplificado hasta agosto de 2026.
--   * GENERAL desde 2026-09-01: TRIBU-CR muestra régimen general desde el 01/09/2026.
-- Idempotente: CREATE TABLE IF NOT EXISTS, INSERT ... WHERE NOT EXISTS por fecha de inicio e INSERT IGNORE del permiso.
-- La hora de registro la escribe la migración con el reloj de la base, que Flyway exige en America/Costa_Rica (V51).

CREATE TABLE IF NOT EXISTS regimen_tributario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    regimen        VARCHAR(12)  NOT NULL,
    vigente_desde  DATE         NOT NULL,
    nota           VARCHAR(300) NULL,
    usuario_id     INT          NULL,
    fecha_registro DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_regimen_tributario_desde UNIQUE (vigente_desde),
    CONSTRAINT ck_regimen_tributario_regimen CHECK (regimen IN ('GENERAL', 'SIMPLIFICADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO regimen_tributario (regimen, vigente_desde, nota, usuario_id, fecha_registro)
SELECT 'SIMPLIFICADO', '2000-01-01',
       'Semilla T-056: régimen simplificado desde antes de cualquier documento del sistema (la primera factura es de 2026-07-04), y el titular estaba en el simplificado hasta agosto de 2026',
       NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM regimen_tributario WHERE vigente_desde = '2000-01-01');

INSERT INTO regimen_tributario (regimen, vigente_desde, nota, usuario_id, fecha_registro)
SELECT 'GENERAL', '2026-09-01',
       'Semilla T-056: TRIBU-CR muestra régimen general desde el 01/09/2026',
       NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM regimen_tributario WHERE vigente_desde = '2026-09-01');

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('REGIMEN_TRIBUTARIO_GESTIONAR', 'Gestionar el régimen tributario',
     'Agregar, corregir y borrar tramos futuros del régimen tributario con vigencia (operación crítica)',
     'Configuración', true, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT r.id_rol, p.id_permiso
    FROM rol r
    JOIN permiso p ON p.codigo = 'REGIMEN_TRIBUTARIO_GESTIONAR'
    WHERE r.codigo = 'ADMIN';
-- fin-permiso
