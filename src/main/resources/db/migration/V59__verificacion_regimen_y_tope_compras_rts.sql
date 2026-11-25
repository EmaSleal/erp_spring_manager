-- T-056 parte 2 (ADR-072): verificación del régimen contra /fe/ae y tope anual de compras del RTS.
--
-- 1. verificacion_regimen: una fila por verificación (manual o del job diario) con el régimen que tenía el ERP, el código y
--    la descripción que devolvió Hacienda y el resultado (COINCIDE, DIFIERE, NO_ENCONTRADO, SIN_RESPUESTA). Retención corta:
--    la aplicación borra las de más de N días (regimen.verificacion.retencion-dias) y siempre conserva la última. Nunca
--    escribe regimen_tributario. Las horas las escribe la aplicación con su Clock (America/Costa_Rica): sin relojes de la base.
-- 2. tope_compras_rts: parámetros por año del tope de compras del RTS (tope = salario_base x multiplicador, con IVA, en CRC).
--    El multiplicador (186, Decreto 43881-H) y los umbrales (80 % de aviso y 100 % de tope) son solo valores por defecto de
--    la columna; no se siembra ningún salario base (el de 2027 no está publicado). nivel_notificado guarda el último nivel
--    avisado del año (BAJO, AVISO, TOPE) para no repetir la notificación cada día.
-- 3. Permiso REGIMEN_TOPE_COMPRAS_GESTIONAR (crítico), asignado al rol ADMIN. Idempotente: INSERT IGNORE.
-- Idempotente: CREATE TABLE IF NOT EXISTS e INSERT IGNORE.

CREATE TABLE IF NOT EXISTS verificacion_regimen (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    fecha_hora           DATETIME     NOT NULL,
    regimen_local        VARCHAR(12)  NULL,
    codigo_hacienda      INT          NULL,
    descripcion_hacienda VARCHAR(200) NULL,
    regimen_hacienda     VARCHAR(12)  NULL,
    resultado            VARCHAR(15)  NOT NULL,
    detalle              VARCHAR(300) NULL,
    usuario_id           INT          NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_verificacion_regimen_resultado CHECK (resultado IN ('COINCIDE', 'DIFIERE', 'NO_ENCONTRADO', 'SIN_RESPUESTA')),
    INDEX idx_verificacion_regimen_fecha (fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS tope_compras_rts (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    anio                INT           NOT NULL,
    salario_base        DECIMAL(15,2) NOT NULL,
    multiplicador       DECIMAL(7,2)  NOT NULL DEFAULT 186.00,
    umbral_aviso        DECIMAL(5,2)  NOT NULL DEFAULT 80.00,
    umbral_tope         DECIMAL(5,2)  NOT NULL DEFAULT 100.00,
    nivel_notificado    VARCHAR(10)   NOT NULL DEFAULT 'BAJO',
    nota                VARCHAR(300)  NULL,
    usuario_id          INT           NULL,
    fecha_registro      DATETIME      NOT NULL,
    fecha_actualizacion DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tope_compras_rts_anio UNIQUE (anio),
    CONSTRAINT ck_tope_compras_rts_salario CHECK (salario_base > 0),
    CONSTRAINT ck_tope_compras_rts_multiplicador CHECK (multiplicador > 0),
    CONSTRAINT ck_tope_compras_rts_umbrales CHECK (umbral_aviso > 0 AND umbral_aviso < umbral_tope AND umbral_tope <= 100),
    CONSTRAINT ck_tope_compras_rts_nivel CHECK (nivel_notificado IN ('BAJO', 'AVISO', 'TOPE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('REGIMEN_TOPE_COMPRAS_GESTIONAR', 'Gestionar el tope de compras del RTS',
     'Registrar y corregir por año el salario base, el multiplicador y los umbrales del tope de compras del régimen simplificado (operación crítica)',
     'Configuración', true, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT r.id_rol, p.id_permiso
    FROM rol r
    JOIN permiso p ON p.codigo = 'REGIMEN_TOPE_COMPRAS_GESTIONAR'
    WHERE r.codigo = 'ADMIN';
-- fin-permiso
