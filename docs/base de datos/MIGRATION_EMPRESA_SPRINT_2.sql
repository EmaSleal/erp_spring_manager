-- =====================================================
-- MIGRATION: EMPRESA - SPRINT 2 FASE 1
-- Proyecto: WhatsApp Orders Manager
-- Descripción: Tabla para almacenar datos de la empresa
-- Fecha: 13 de octubre de 2025
-- =====================================================

-- Verificar si la tabla ya existe
-- Si existe, no crear nuevamente
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'empresa'
);

-- =====================================================
-- CREACIÓN DE TABLA EMPRESA
-- =====================================================

CREATE TABLE IF NOT EXISTS empresa (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID único de la empresa',
    
    -- Información básica
    nombre_empresa VARCHAR(200) NOT NULL COMMENT 'Razón social o nombre legal de la empresa',
    nombre_comercial VARCHAR(200) NULL COMMENT 'Nombre comercial (opcional)',
    ruc VARCHAR(20) NULL COMMENT 'RUC / NIT / CUIT / RFC según país',
    
    -- Datos de contacto
    direccion VARCHAR(300) NULL COMMENT 'Dirección fiscal',
    telefono VARCHAR(20) NULL COMMENT 'Teléfono principal',
    email VARCHAR(100) NULL COMMENT 'Email de contacto',
    sitio_web VARCHAR(200) NULL COMMENT 'URL del sitio web',
    
    -- Recursos visuales
    logo VARCHAR(255) NULL COMMENT 'Ruta del archivo del logo',
    favicon VARCHAR(255) NULL COMMENT 'Ruta del archivo del favicon',
    
    -- Control
    activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Indica si está activo',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    fecha_modificacion TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última modificación',
    usuario_modificacion INT NULL COMMENT 'ID del usuario que modificó',
    
    -- Índices
    INDEX idx_activo (activo),
    INDEX idx_ruc (ruc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Datos de la empresa';

-- =====================================================
-- INSERTAR CONFIGURACIÓN POR DEFECTO
-- =====================================================

-- Solo insertar si la tabla está vacía
INSERT INTO empresa (
    nombre_empresa, 
    nombre_comercial, 
    ruc,
    direccion,
    telefono,
    email,
    activo
)
SELECT 
    'Mi Empresa S.A.C.',
    'Mi Empresa',
    '20123456789',
    'Av. Principal 123, Lima, Perú',
    '+51 999 999 999',
    'contacto@miempresa.com',
    TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM empresa LIMIT 1);

-- =====================================================
-- VERIFICACIÓN
-- =====================================================

-- Mostrar el registro insertado
SELECT * FROM empresa;

-- =====================================================
-- NOTAS IMPORTANTES
-- =====================================================

/*
1. SOLO UN REGISTRO ACTIVO:
   - Debe existir solo un registro con activo = TRUE
   - La aplicación debe validar esto en el servicio

2. LOGOS Y FAVICONS:
   - Se guardarán en: /uploads/empresa/
   - Formatos permitidos: PNG, JPG, JPEG, SVG
   - Tamaño máximo: 2MB

3. CAMPOS OPCIONALES:
   - nombre_comercial: si es igual al legal, dejar NULL
   - ruc, direccion, telefono, email, sitio_web: opcionales
   - logo, favicon: opcionales

4. INTEGRACIÓN:
   - Se usará en FacturaController para PDFs
   - Se usará en navbar para mostrar logo
   - Se usará en <head> para favicon

5. MIGRACIÓN:
   - Si ya tienes datos, ejecutar este script no los afectará
   - Si la tabla no existe, se creará con datos por defecto

6. USUARIO_MODIFICACION:
   - Se llenará desde la aplicación con el ID del usuario logueado
   - No tiene FK porque puede ser NULL en la creación inicial
*/

-- =====================================================
-- ROLLBACK (Solo si es necesario)
-- =====================================================

-- DROP TABLE IF EXISTS empresa;
