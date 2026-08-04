-- V4__rrhh_catalogos_regulatorios.sql
-- Catálogos regulatorios versionables: CCSS, renta (impuesto al salario), salarios mínimos.
-- Data: Decreto 45333-H (renta 2026) y Decreto 45303-MTSS (salarios mínimos 2026).

CREATE TABLE parametros_ccss (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    porcentaje_obrero               DECIMAL(5,4) NOT NULL,
    porcentaje_patronal             DECIMAL(5,4) NOT NULL,
    porcentaje_sem                  DECIMAL(5,4) NOT NULL,
    porcentaje_ivm_obrero           DECIMAL(5,4) NOT NULL,
    porcentaje_bp_obrero            DECIMAL(5,4) NOT NULL,
    porcentaje_fcl                  DECIMAL(5,4) NOT NULL,
    porcentaje_rop                  DECIMAL(5,4) NOT NULL,
    base_minima_contributiva_sem    DECIMAL(19,2) NOT NULL,
    base_minima_contributiva_ivm    DECIMAL(19,2) NOT NULL,
    vigencia_desde                  DATE         NOT NULL,
    vigencia_hasta                  DATE,
    created_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ccss_vigencia (vigencia_desde, vigencia_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tasas vigentes 1-ene-2026 hasta 31-dic-2028 (IVM sube de nuevo el 1-ene-2029)
-- Obrero: SEM 5.50% + IVM 4.33% + BP 1.00% = 10.83%
-- Patronal total: 26.83% (para empresas con 5+ empleados, sector no agrícola)
-- FCL: 1.50%  |  ROP patronal: 2.00%
INSERT IGNORE INTO parametros_ccss (
    porcentaje_obrero, porcentaje_patronal,
    porcentaje_sem, porcentaje_ivm_obrero, porcentaje_bp_obrero,
    porcentaje_fcl, porcentaje_rop,
    base_minima_contributiva_sem, base_minima_contributiva_ivm,
    vigencia_desde, vigencia_hasta
) VALUES (
    0.1083, 0.2683,
    0.0550, 0.0433, 0.0100,
    0.0150, 0.0200,
    333328.00, 311990.00,
    '2026-01-01', '2028-12-31'
);

CREATE TABLE tramos_impuesto_salario (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    anio_vigencia        INT           NOT NULL,
    limite_inferior      DECIMAL(19,2) NOT NULL,
    limite_superior      DECIMAL(19,2),
    porcentaje           DECIMAL(5,4)  NOT NULL,
    credito_por_hijo     DECIMAL(19,2),
    credito_por_conyuge  DECIMAL(19,2),
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tramo_anio (anio_vigencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tramos 2026 (Decreto 45333-H, vigente 1-ene-2026)
-- Créditos fiscales mensuales: ₡1,710/hijo | ₡2,590/cónyuge — almacenados en la fila exenta (limiteInferior=0)
INSERT IGNORE INTO tramos_impuesto_salario (anio_vigencia, limite_inferior, limite_superior, porcentaje, credito_por_hijo, credito_por_conyuge) VALUES
(2026,       0.00,   918000.00, 0.0000, 1710.00, 2590.00),
(2026,  918000.00,  1347000.00, 0.1000,    NULL,     NULL),
(2026, 1347000.00,  2364000.00, 0.1500,    NULL,     NULL),
(2026, 2364000.00,  4727000.00, 0.2000,    NULL,     NULL),
(2026, 4727000.00,         NULL, 0.2500,    NULL,     NULL);

CREATE TABLE salarios_minimos (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria             VARCHAR(20)   NOT NULL,
    descripcion_categoria VARCHAR(200)  NOT NULL,
    monto_mensual         DECIMAL(19,2) NOT NULL,
    vigencia_desde        DATE          NOT NULL,
    vigencia_hasta        DATE,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_smin_categoria (categoria),
    INDEX idx_smin_vigencia (vigencia_desde, vigencia_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Salarios mínimos 2026 (Decreto 45303-MTSS, vigente 1-ene-2026 hasta 30-jun-2026)
INSERT IGNORE INTO salarios_minimos (categoria, descripcion_categoria, monto_mensual, vigencia_desde, vigencia_hasta) VALUES
('TONC', 'Trabajador No Calificado Genérico',  373092.30, '2026-01-01', '2026-06-30'),
('TOSC', 'Trabajador Semi Calificado',          396000.00, '2026-01-01', '2026-06-30'),
('TOC',  'Trabajador Calificado',               420000.00, '2026-01-01', '2026-06-30'),
('TES',  'Técnico de Educación Secundaria',     530000.00, '2026-01-01', '2026-06-30'),
('TOE',  'Universitario / Especializado',       796921.00, '2026-01-01', '2026-06-30'),
('DOM',  'Trabajo doméstico',                   268731.31, '2026-01-01', '2026-06-30');
