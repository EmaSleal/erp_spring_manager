-- ============================================================================
-- V3 — RRHH: Catálogos Base (Departamento + Puesto)
-- Additive only — no DROP/ALTER on existing tables
-- ============================================================================

CREATE TABLE IF NOT EXISTS departamentos (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(200) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_dept_nombre UNIQUE (nombre)
);

CREATE INDEX idx_dept_activo ON departamentos (activo);
CREATE INDEX idx_dept_nombre ON departamentos (nombre);

CREATE TABLE IF NOT EXISTS puestos (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    nombre                      VARCHAR(200) NOT NULL,
    descripcion                 VARCHAR(500),
    salario_base                DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    categoria_salarial_minima   VARCHAR(100),
    tipo_jornada                VARCHAR(20),
    departamento_id             BIGINT NOT NULL,
    activo                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  DATETIME NOT NULL,
    updated_at                  DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_puesto_departamento FOREIGN KEY (departamento_id) REFERENCES departamentos(id)
);

CREATE INDEX idx_puesto_departamento ON puestos (departamento_id);
CREATE INDEX idx_puesto_activo ON puestos (activo);

-- ============================================================================
-- Seed — 5 departamentos iniciales
-- ============================================================================
INSERT IGNORE INTO departamentos (nombre, activo, created_at, updated_at) VALUES
    ('Recursos Humanos',    TRUE, NOW(), NOW()),
    ('Tecnología',          TRUE, NOW(), NOW()),
    ('Ventas',              TRUE, NOW(), NOW()),
    ('Administración',      TRUE, NOW(), NOW()),
    ('Operaciones',         TRUE, NOW(), NOW());
