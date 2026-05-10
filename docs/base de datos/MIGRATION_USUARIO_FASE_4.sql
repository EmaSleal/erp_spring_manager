-- ============================================================================
-- MIGRACIÓN: Ampliar tabla Usuario para Fase 4 - Perfil de Usuario
-- Sprint 1 - Punto 4.1
-- Fecha: 12 de octubre de 2025
-- ============================================================================

USE whats_orders_manager;

-- Agregar columna email (única)
ALTER TABLE usuario
ADD COLUMN email VARCHAR(100) NULL UNIQUE
COMMENT 'Correo electrónico del usuario'
AFTER telefono;

-- Agregar columna avatar (path o URL de imagen)
ALTER TABLE usuario
ADD COLUMN avatar VARCHAR(255) NULL
COMMENT 'URL o ruta del avatar del usuario'
AFTER rol;

-- Agregar columna activo (estado del usuario)
ALTER TABLE usuario
ADD COLUMN activo BOOLEAN DEFAULT TRUE NOT NULL
COMMENT 'Indica si el usuario está activo en el sistema'
AFTER avatar;

-- Agregar columna ultimo_acceso (timestamp del último login)
ALTER TABLE usuario
ADD COLUMN ultimo_acceso TIMESTAMP NULL
COMMENT 'Fecha y hora del último acceso del usuario'
AFTER activo;

-- ============================================================================
-- VALIDACIÓN: Verificar que las columnas se crearon correctamente
-- ============================================================================

-- Ver estructura de la tabla
DESCRIBE usuario;

-- Ver todos los usuarios con las nuevas columnas
SELECT 
    id_usuario,
    nombre,
    telefono,
    email,
    rol,
    avatar,
    activo,
    ultimo_acceso,
    createDate,
    updateDate
FROM usuario;

-- ============================================================================
-- DATOS DE EJEMPLO (OPCIONAL)
-- ============================================================================

-- Actualizar usuarios existentes con datos de ejemplo
UPDATE usuario
SET 
    email = CONCAT(LOWER(REPLACE(nombre, ' ', '.')), '@whatsorders.com'),
    activo = TRUE,
    ultimo_acceso = NULL
WHERE email IS NULL;

-- ============================================================================
-- ÍNDICES (OPCIONAL - para mejorar rendimiento)
-- ============================================================================

-- Índice en email para búsquedas rápidas
CREATE INDEX idx_usuario_email ON usuario(email);

-- Índice en activo para filtrar usuarios activos/inactivos
CREATE INDEX idx_usuario_activo ON usuario(activo);

-- Índice compuesto para consultas de login
CREATE INDEX idx_usuario_login ON usuario(telefono, activo);

-- ============================================================================
-- NOTAS
-- ============================================================================

/*
NUEVOS CAMPOS AGREGADOS:

1. email (VARCHAR 100, UNIQUE, NULL)
   - Correo electrónico del usuario
   - Único en el sistema
   - Nullable para no romper datos existentes
   - Se puede usar para login o recuperación de contraseña

2. avatar (VARCHAR 255, NULL)
   - URL o path del archivo de imagen de perfil
   - Ejemplo: "/images/avatars/usuario123.jpg"
   - Ejemplo: "https://gravatar.com/avatar/..."
   - NULL = usar avatar por defecto (iniciales)

3. activo (BOOLEAN, NOT NULL, DEFAULT TRUE)
   - TRUE = Usuario puede acceder al sistema
   - FALSE = Usuario deshabilitado (soft delete)
   - Útil para suspender usuarios sin eliminarlos

4. ultimo_acceso (TIMESTAMP, NULL)
   - Se actualiza cada vez que el usuario hace login exitoso
   - Útil para:
     * Auditoría de seguridad
     * Detectar usuarios inactivos
     * Estadísticas de uso
   - NULL = usuario nunca ha hecho login

IMPACTO:
- No rompe funcionalidad existente (todos los campos son NULL o tienen DEFAULT)
- Requiere actualizar UserDetailsServiceImpl para registrar ultimo_acceso
- Requiere actualizar AuthController para manejar email
- Permite implementar módulo de Perfil de Usuario completo

PRÓXIMOS PASOS:
1. Ejecutar este script en la base de datos
2. Reiniciar la aplicación para que Hibernate detecte los cambios
3. Implementar PerfilController (punto 4.2)
4. Crear vistas de perfil (puntos 4.3 y 4.4)
*/

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================
