-- ============================================================================
-- V55: cuentas por pagar por líneas con cuenta y tratamiento de IVA, prorrata provisional por año y aceptación del
--      comprobante del proveedor (T-054, ADR-067 §2-4, P-020)
-- ============================================================================
-- 1. cuenta_pagar_linea reemplaza a cuenta_pagar_iva (V53). Cada línea de una CxP manual lleva su cuenta contable (hoja),
--    la tarifa (CodigoTarifaIVA de Hacienda v4.4; NULL = «sin tarifa», solo en las filas migradas), la base y el IVA en la
--    moneda de la CxP, el tratamiento del IVA (PLENO, USO_COMUN, LIMITADO, NO_ACREDITABLE), la tarifa límite (solo
--    LIMITADO, en %) y el porcentaje de prorrata del año aplicado al crearla (solo USO_COMUN: así corregir el porcentaje
--    de un año no cambia la historia). iva_acreditable e iva_no_acreditable se calculan y se guardan; el no acreditable
--    se suma a la cuenta de la línea en el asiento.
--
--    Migración de las filas existentes: una línea por fila de cuenta_pagar_iva, numerada por orden de id dentro de la CxP,
--    con tratamiento PLENO (acreditable = IVA, no acreditable = 0), que es lo que el asiento anterior hizo con todo el
--    IVA. Cuenta de la línea = cuenta_gasto_id de la CxP o, si no tiene (CxP de orden de compra, anteriores a V28),
--    la de CPP_CUENTA_COMPRAS: es la que debitó su asiento, no se inventa. Si una CxP no tuviera ninguna de las dos la
--    migración aborta (cuenta_contable_id NOT NULL) en lugar de adivinar. Descripción = la de la CxP.
--    En el dump de producción hay 0 filas que migrar.
--
--    cuenta_pagar_iva se elimina. cuentas_por_pagar.cuenta_gasto_id queda en desuso (nullable; no se borra ni se usa en
--    las CxP nuevas).
--
-- 2. prorrata_iva: porcentaje provisional de prorrata del IVA por año (0 a 100, 2 decimales).
--
-- 3. cuentas_por_pagar: aceptación del comprobante con mensaje receptor (estado PENDIENTE por defecto, fecha y número).
--
-- 4. Permiso PRORRATA_IVA_GESTIONAR (crítico), asignado al rol ADMIN. Idempotente: INSERT IGNORE.
-- ============================================================================

CREATE TABLE cuenta_pagar_linea (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    cuenta_por_pagar_id BIGINT        NOT NULL,
    numero_linea        INT           NOT NULL,
    descripcion         VARCHAR(300)  NOT NULL,
    cuenta_contable_id  BIGINT        NOT NULL,
    codigo_tarifa       VARCHAR(2)    NULL,
    base                DECIMAL(15,2) NOT NULL,
    iva                 DECIMAL(15,2) NOT NULL,
    tratamiento         VARCHAR(20)   NOT NULL,
    tarifa_limite       DECIMAL(5,2)  NULL,
    porcentaje_prorrata DECIMAL(5,2)  NULL,
    iva_acreditable     DECIMAL(15,2) NOT NULL,
    iva_no_acreditable  DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cpl_cuenta_pagar FOREIGN KEY (cuenta_por_pagar_id) REFERENCES cuentas_por_pagar (id),
    CONSTRAINT fk_cpl_cuenta_contable FOREIGN KEY (cuenta_contable_id) REFERENCES cuentas_contables (id),
    CONSTRAINT uk_cpl_cuenta_pagar_numero UNIQUE (cuenta_por_pagar_id, numero_linea),
    CONSTRAINT ck_cpl_tratamiento CHECK (tratamiento IN ('PLENO', 'USO_COMUN', 'LIMITADO', 'NO_ACREDITABLE')),
    KEY idx_cpl_cuenta_contable (cuenta_contable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO cuenta_pagar_linea
    (cuenta_por_pagar_id, numero_linea, descripcion, cuenta_contable_id, codigo_tarifa, base, iva, tratamiento,
     tarifa_limite, porcentaje_prorrata, iva_acreditable, iva_no_acreditable)
    SELECT i.cuenta_por_pagar_id,
           ROW_NUMBER() OVER (PARTITION BY i.cuenta_por_pagar_id ORDER BY i.id),
           COALESCE(NULLIF(TRIM(c.descripcion), ''), 'Migrado del desglose'),
           COALESCE(c.cuenta_gasto_id,
                    (SELECT p.cuenta_contable_id FROM parametros_contables p WHERE p.clave = 'CPP_CUENTA_COMPRAS')),
           i.codigo_tarifa, i.base, i.iva, 'PLENO', NULL, NULL, i.iva, 0
    FROM cuenta_pagar_iva i
    JOIN cuentas_por_pagar c ON c.id = i.cuenta_por_pagar_id;

DROP TABLE cuenta_pagar_iva;

CREATE TABLE prorrata_iva (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    anio                 INT          NOT NULL,
    porcentaje           DECIMAL(5,2) NOT NULL,
    nota                 VARCHAR(300) NULL,
    usuario_id           INT          NULL,
    fecha_registro       DATETIME     NOT NULL,
    fecha_actualizacion  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_prorrata_iva_anio UNIQUE (anio),
    CONSTRAINT ck_prorrata_iva_porcentaje CHECK (porcentaje >= 0 AND porcentaje <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE cuentas_por_pagar
    ADD COLUMN estado_aceptacion        VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN fecha_aceptacion         DATE        NULL,
    ADD COLUMN numero_mensaje_receptor  VARCHAR(50) NULL;

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('PRORRATA_IVA_GESTIONAR', 'Gestionar la prorrata del IVA',
     'Registrar y corregir el porcentaje provisional de prorrata del IVA por año (operación crítica)',
     'Contabilidad', true, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT r.id_rol, p.id_permiso
    FROM rol r
    JOIN permiso p ON p.codigo = 'PRORRATA_IVA_GESTIONAR'
    WHERE r.codigo = 'ADMIN';
-- fin-permiso
