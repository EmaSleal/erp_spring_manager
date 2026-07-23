-- ============================================================================
-- V6: RRHH — Contratos de Empleado y Ausencias
-- ============================================================================
-- Depends on: V5 (empleados table), V1 (usuario table)
-- ============================================================================

-- ── contratos_empleado ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS contratos_empleado (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    empleado_id              BIGINT       NOT NULL,
    tipo_contrato            VARCHAR(50)  NOT NULL,
    fecha_inicio             DATE         NOT NULL,
    fecha_fin                DATE,
    salario_bruto            DECIMAL(15,2) NOT NULL,
    jornada                  VARCHAR(50),
    cargo_contratado         VARCHAR(150),
    causa_terminacion        VARCHAR(80),
    fecha_terminacion        DATE,
    descripcion_terminacion  TEXT,
    activo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_contrato_empleado
        FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

CREATE INDEX idx_contratos_empleado_id ON contratos_empleado (empleado_id);

-- ── ausencias ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS ausencias (
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    empleado_id               BIGINT      NOT NULL,
    tipo_ausencia             VARCHAR(50) NOT NULL,
    fecha_inicio              DATE        NOT NULL,
    fecha_fin                 DATE        NOT NULL,
    descripcion               TEXT,
    con_goce_salario          BOOLEAN     NOT NULL DEFAULT TRUE,
    computa_para_aguinaldo    BOOLEAN     NOT NULL DEFAULT TRUE,
    computa_antiguedad        BOOLEAN     NOT NULL DEFAULT TRUE,
    justificada               BOOLEAN     NOT NULL DEFAULT TRUE,
    aprobada                  BOOLEAN     NOT NULL DEFAULT FALSE,
    aprobada_por              INT,
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_ausencia_empleado
        FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    CONSTRAINT fk_ausencia_aprobada_por
        FOREIGN KEY (aprobada_por) REFERENCES usuario(id_usuario)
);

CREATE INDEX idx_ausencias_empleado_id ON ausencias (empleado_id);
