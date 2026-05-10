-- ============================================================
-- TABLAS DE CATÁLOGOS: UBICACIONES DE COSTA RICA
-- ============================================================
-- Estructura para división territorial según Ministerio de Hacienda
-- Requerido para cumplimiento Facturación Electrónica v4.4

-- Tabla: Provincias
CREATE TABLE IF NOT EXISTS cat_provincia_cr (
    codigo CHAR(1) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_provincia_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de 7 provincias de Costa Rica';

-- Tabla: Cantones
CREATE TABLE IF NOT EXISTS cat_canton_cr (
    provincia_codigo CHAR(1) NOT NULL,
    codigo CHAR(2) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provincia_codigo, codigo),
    FOREIGN KEY (provincia_codigo) REFERENCES cat_provincia_cr(codigo) ON DELETE CASCADE,
    INDEX idx_canton_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de cantones de Costa Rica (82 cantones)';

-- Tabla: Distritos
CREATE TABLE IF NOT EXISTS cat_distrito_cr (
    provincia_codigo CHAR(1) NOT NULL,
    canton_codigo CHAR(2) NOT NULL,
    codigo CHAR(2) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provincia_codigo, canton_codigo, codigo),
    FOREIGN KEY (provincia_codigo, canton_codigo) 
        REFERENCES cat_canton_cr(provincia_codigo, codigo) ON DELETE CASCADE,
    INDEX idx_distrito_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de distritos de Costa Rica (~487 distritos)';

-- Tabla: Barrios (Opcional)
CREATE TABLE IF NOT EXISTS cat_barrio_cr (
    provincia_codigo CHAR(1) NOT NULL,
    canton_codigo CHAR(2) NOT NULL,
    distrito_codigo CHAR(2) NOT NULL,
    codigo CHAR(2) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provincia_codigo, canton_codigo, distrito_codigo, codigo),
    FOREIGN KEY (provincia_codigo, canton_codigo, distrito_codigo) 
        REFERENCES cat_distrito_cr(provincia_codigo, canton_codigo, codigo) ON DELETE CASCADE,
    INDEX idx_barrio_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de barrios de Costa Rica (opcional según Hacienda)';

-- Vista: Ubicación completa
CREATE OR REPLACE VIEW cat_ubicacion_cr AS
SELECT 
    p.codigo AS provincia_codigo,
    p.nombre AS provincia_nombre,
    c.codigo AS canton_codigo,
    c.nombre AS canton_nombre,
    d.codigo AS distrito_codigo,
    d.nombre AS distrito_nombre,
    CONCAT(p.codigo, '-', c.codigo, '-', d.codigo) AS codigo_completo,
    CONCAT(p.nombre, ', ', c.nombre, ', ', d.nombre) AS ubicacion_completa
FROM cat_provincia_cr p
LEFT JOIN cat_canton_cr c ON p.codigo = c.provincia_codigo
LEFT JOIN cat_distrito_cr d ON c.provincia_codigo = d.provincia_codigo 
    AND c.codigo = d.canton_codigo
ORDER BY p.codigo, c.codigo, d.codigo;

-- Verificación
SELECT 'Tablas de ubicaciones CR creadas exitosamente' AS status;
