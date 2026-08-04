-- ============================================================================
-- V12: Nómina — tablas nominas y detalles_nomina
-- ============================================================================
-- Depends on: V5 (empleados table)
-- ============================================================================

-- ── nominas ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS nominas (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    numero               VARCHAR(50)   NOT NULL,
    periodo_inicio       DATE          NOT NULL,
    periodo_fin          DATE          NOT NULL,
    fecha_pago           DATE          NOT NULL,
    tipo                 VARCHAR(20)   NOT NULL,
    estado               VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR',
    total_bruto          DECIMAL(15,2)          DEFAULT 0.00,
    total_deducciones    DECIMAL(15,2)          DEFAULT 0.00,
    total_neto           DECIMAL(15,2)          DEFAULT 0.00,
    total_ccss_patronal  DECIMAL(15,2)          DEFAULT 0.00,
    motivo_anulacion     TEXT,
    created_by           VARCHAR(255),
    created_date         DATETIME,
    last_modified_by     VARCHAR(255),
    last_modified_date   DATETIME,

    PRIMARY KEY (id),
    CONSTRAINT uk_nomina_numero   UNIQUE (numero),
    CONSTRAINT uk_nomina_periodo  UNIQUE (periodo_inicio, periodo_fin, tipo)
);

CREATE INDEX idx_nomina_estado ON nominas (estado);

-- ── detalles_nomina ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS detalles_nomina (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    nomina_id            BIGINT        NOT NULL,
    empleado_id          BIGINT        NOT NULL,
    salario_base         DECIMAL(15,2) NOT NULL,
    horas_ausentes       DECIMAL(8,2)           DEFAULT 0.00,
    bruto_prorrateado    DECIMAL(15,2)          DEFAULT 0.00,
    ccss_obrero          DECIMAL(15,2)          DEFAULT 0.00,
    ins                  DECIMAL(15,2)          DEFAULT 0.00,
    impuesto_renta       DECIMAL(15,2)          DEFAULT 0.00,
    credito_familiar     DECIMAL(15,2)          DEFAULT 0.00,
    solidarista          DECIMAL(15,2)          DEFAULT 0.00,
    pension_alimentaria  DECIMAL(15,2)          DEFAULT 0.00,
    otras_deducciones    DECIMAL(15,2)          DEFAULT 0.00,
    total_deducciones    DECIMAL(15,2)          DEFAULT 0.00,
    salario_neto         DECIMAL(15,2)          DEFAULT 0.00,
    ccss_patronal        DECIMAL(15,2)          DEFAULT 0.00,

    PRIMARY KEY (id),
    CONSTRAINT uk_nomina_empleado UNIQUE (nomina_id, empleado_id),
    CONSTRAINT fk_detalle_nomina
        FOREIGN KEY (nomina_id) REFERENCES nominas(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_empleado
        FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

CREATE INDEX idx_detalle_nomina   ON detalles_nomina (nomina_id);
CREATE INDEX idx_detalle_empleado ON detalles_nomina (empleado_id);
