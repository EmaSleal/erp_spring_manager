# 🤖 Trigger Automático de Preferencias de Notificación

## 📋 Descripción

Este sistema automatiza completamente la creación de preferencias de notificación cada vez que se crea un nuevo usuario en el sistema.

## ✨ Características

- ✅ **100% Automático**: No necesitas ejecutar scripts manualmente
- ✅ **Consistente**: Todos los usuarios tendrán preferencias desde el momento de su creación
- ✅ **Inteligente**: Crea preferencias según disponibilidad de email/teléfono
- ✅ **Actualizable**: Si un usuario agrega email/teléfono después, se crean las preferencias correspondientes
- ✅ **Reutilizable**: Procedimiento almacenado que puede llamarse manualmente

## 📦 Componentes

### 1. Stored Procedure: `sp_crear_preferencias_usuario`

Procedimiento que contiene la lógica para crear todas las preferencias de un usuario.

**Parámetros:**
- `p_id_usuario` (INT): ID del usuario para el cual crear preferencias

**Crea:**
- Mínimo 4 preferencias (si solo tiene nombre/teléfono básico)
- Hasta 7 preferencias (si tiene email y teléfono completos)

### 2. Trigger: `trg_after_insert_usuario`

Se ejecuta automáticamente **DESPUÉS** de insertar un nuevo usuario.

**Condición:** Solo crea preferencias si `usuario.activo = true`

### 3. Trigger: `trg_after_update_usuario`

Se ejecuta automáticamente **DESPUÉS** de actualizar un usuario.

**Funcionalidad:**
- Si se agrega EMAIL → crea preferencias EMAIL
- Si se agrega TELÉFONO → crea preferencias WHATSAPP

## 📊 Preferencias Creadas

### Para TODOS los usuarios:

| Tipo | Canal | Frecuencia | Descripción |
|------|-------|------------|-------------|
| NULL (Global) | NULL | INMEDIATA | Preferencia base para todo |
| FACTURA_VENCIDA | WEB | INMEDIATA | Alertas web de facturas vencidas |
| STOCK_BAJO | WEB | INMEDIATA | Alertas web de stock bajo |
| NUEVA_FACTURA | WEB | INMEDIATA | Notificación de nuevas facturas |

### Si tiene EMAIL:

| Tipo | Canal | Frecuencia | Descripción |
|------|-------|------------|-------------|
| FACTURA_VENCIDA | EMAIL | INMEDIATA | Email de facturas vencidas |
| STOCK_BAJO | EMAIL | DIARIA | Resumen diario de stock bajo |

### Si tiene TELÉFONO:

| Tipo | Canal | Frecuencia | Descripción |
|------|-------|------------|-------------|
| FACTURA_VENCIDA | WHATSAPP | INMEDIATA | WhatsApp de facturas vencidas |

## 🚀 Instalación

### 1. Ejecutar el Script

```sql
-- Conectarse a la base de datos
USE whats_orders_manager;

-- Ejecutar el script completo
source /ruta/a/TRIGGER_PREFERENCIAS_USUARIO.sql;
```

O copiar y pegar el contenido del archivo `TRIGGER_PREFERENCIAS_USUARIO.sql` en tu gestor de BD.

### 2. Verificar Instalación

```sql
-- Ver los triggers creados
SHOW TRIGGERS LIKE 'usuario';

-- Ver el procedimiento almacenado
SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name = 'sp_crear_preferencias_usuario';
```

**Resultado esperado:**
- 2 triggers: `trg_after_insert_usuario`, `trg_after_update_usuario`
- 1 procedimiento: `sp_crear_preferencias_usuario`

## 🧪 Pruebas

### Prueba 1: Crear un Usuario Nuevo

```sql
INSERT INTO usuario (
    nombre,
    telefono,
    email,
    password,
    rol,
    activo,
    bloqueado,
    require_cambio_password,
    create_date,
    update_date
) VALUES (
    'Test Automático',
    '98765432',
    'test.auto@example.com',
    '$2a$10$defaulthash',
    'VENDEDOR',
    true,
    false,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Verificar preferencias creadas
SELECT 
    u.nombre,
    pn.tipo_notificacion,
    pn.canal,
    pn.activa,
    pn.frecuencia
FROM usuario u
INNER JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
WHERE u.nombre = 'Test Automático'
ORDER BY pn.tipo_notificacion, pn.canal;
```

**Resultado esperado:** 7 preferencias creadas automáticamente

### Prueba 2: Agregar Email a Usuario Sin Email

```sql
-- Supongamos que el usuario con ID 5 no tiene email
UPDATE usuario 
SET email = 'nuevo.email@example.com'
WHERE id_usuario = 5;

-- Verificar que se crearon preferencias EMAIL
SELECT * FROM preferencia_notificacion
WHERE id_usuario = 5 AND canal = 'EMAIL';
```

**Resultado esperado:** 2 nuevas preferencias EMAIL creadas

## 🔧 Uso Manual del Procedimiento

### Para un usuario específico:

```sql
-- Crear preferencias para usuario con ID 10
CALL sp_crear_preferencias_usuario(10);
```

### Para todos los usuarios sin preferencias:

```sql
-- Primero crear el procedimiento auxiliar (incluido en el script)
CALL sp_inicializar_preferencias_todos();
```

Este procedimiento:
1. Busca todos los usuarios activos sin preferencias
2. Llama a `sp_crear_preferencias_usuario` para cada uno
3. Crea todas las preferencias automáticamente

## 📈 Estadísticas y Verificación

### Ver preferencias por usuario:

```sql
SELECT 
    u.id_usuario,
    u.nombre,
    u.email IS NOT NULL AS tiene_email,
    u.telefono IS NOT NULL AS tiene_telefono,
    COUNT(pn.id_preferencia) AS total_preferencias,
    SUM(CASE WHEN pn.activa = true THEN 1 ELSE 0 END) AS preferencias_activas
FROM usuario u
LEFT JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
WHERE u.activo = true
GROUP BY u.id_usuario, u.nombre, u.email, u.telefono
ORDER BY u.id_usuario;
```

### Ver distribución de preferencias por tipo y canal:

```sql
SELECT 
    tipo_notificacion,
    canal,
    COUNT(*) AS total_usuarios,
    SUM(CASE WHEN activa = true THEN 1 ELSE 0 END) AS usuarios_activos
FROM preferencia_notificacion
GROUP BY tipo_notificacion, canal
ORDER BY tipo_notificacion, canal;
```

## 🔄 Integración con Spring Boot

El trigger funciona en conjunto con el método `crearPreferenciasPredeterminadas()` del service:

**¿Cuál usar?**

- **Trigger SQL**: Automático, funciona siempre (incluso con inserts SQL directos)
- **Service Java**: Más control, puede personalizar lógica por rol

**Recomendación:** Mantener ambos:
- Trigger para usuarios creados por SQL/importación
- Service para usuarios creados desde la aplicación (puede desactivarse el trigger si se prefiere)

## 🛡️ Seguridad y Prevención de Duplicados

El trigger usa `INSERT IGNORE` en las actualizaciones para prevenir duplicados si:
- El trigger se ejecuta múltiples veces
- El usuario ya tiene preferencias creadas manualmente
- Hay conflicto con el constraint `uk_usuario_tipo_canal`

## 🗑️ Desinstalación (si es necesario)

```sql
-- Eliminar triggers
DROP TRIGGER IF EXISTS trg_after_insert_usuario;
DROP TRIGGER IF EXISTS trg_after_update_usuario;

-- Eliminar procedimiento almacenado
DROP PROCEDURE IF EXISTS sp_crear_preferencias_usuario;
DROP PROCEDURE IF EXISTS sp_inicializar_preferencias_todos;
```

## ⚠️ Consideraciones Importantes

1. **Performance**: El trigger agrega ~7 inserts por cada usuario nuevo (aceptable)

2. **Transacciones**: El trigger se ejecuta dentro de la misma transacción del INSERT del usuario
   - Si el trigger falla, el usuario NO se crea
   - Esto garantiza consistencia de datos

3. **Email/Teléfono Opcionales**: El sistema es inteligente:
   - Usuario sin email → No crea preferencias EMAIL
   - Usuario sin teléfono → No crea preferencias WHATSAPP
   - Usuario agrega datos después → Trigger de UPDATE los crea

4. **Compatibilidad**: Compatible con:
   - MySQL 5.7+
   - MariaDB 10.2+
   - Requiere privilegios de CREATE TRIGGER y CREATE PROCEDURE

## 🎯 Casos de Uso

### Caso 1: Nuevo Usuario desde la Aplicación
```java
// En UsuarioService.java
Usuario nuevoUsuario = new Usuario();
nuevoUsuario.setNombre("Juan");
nuevoUsuario.setEmail("juan@example.com");
usuarioRepository.save(nuevoUsuario);

// ✅ Trigger SQL crea preferencias automáticamente
// ✅ No necesitas llamar a crearPreferenciasPredeterminadas()
```

### Caso 2: Importación Masiva de Usuarios
```sql
INSERT INTO usuario (nombre, telefono, email, rol, activo, ...)
VALUES 
    ('User1', '111', 'user1@mail.com', 'VENDEDOR', true, ...),
    ('User2', '222', 'user2@mail.com', 'VENDEDOR', true, ...),
    ('User3', '333', 'user3@mail.com', 'GERENTE', true, ...);

-- ✅ Trigger crea preferencias para los 3 usuarios automáticamente
```

### Caso 3: Usuario Actualiza su Perfil
```java
// Usuario agrega email después de registrarse
usuario.setEmail("nuevo@email.com");
usuarioRepository.save(usuario);

// ✅ Trigger de UPDATE crea preferencias EMAIL automáticamente
```

## 📚 Archivos Relacionados

- `TRIGGER_PREFERENCIAS_USUARIO.sql` - Script principal con triggers y SP
- `INIT_PREFERENCIAS_NOTIFICACION.sql` - Script de inicialización manual
- `PreferenciaNotificacionServiceImpl.java` - Método Java equivalente
- `NotificacionRestController.java` - Endpoint REST de inicialización

## 🆘 Troubleshooting

### Problema: "Trigger already exists"
**Solución:**
```sql
DROP TRIGGER IF EXISTS trg_after_insert_usuario;
-- Luego volver a crear
```

### Problema: "No se crean preferencias"
**Verificar:**
1. Trigger está instalado: `SHOW TRIGGERS LIKE 'usuario';`
2. Usuario está activo: `SELECT activo FROM usuario WHERE id_usuario = X;`
3. Ver logs de MySQL para errores del trigger

### Problema: "Duplicate entry"
**Causa:** Usuario ya tiene preferencias  
**Solución:** Normal, el trigger usa INSERT IGNORE para prevenir esto

## ✅ Checklist de Implementación

- [ ] Ejecutar script `TRIGGER_PREFERENCIAS_USUARIO.sql`
- [ ] Verificar triggers con `SHOW TRIGGERS`
- [ ] Verificar procedimiento con `SHOW PROCEDURE STATUS`
- [ ] Hacer prueba: insertar usuario de prueba
- [ ] Verificar que se crearon 4-7 preferencias
- [ ] Ejecutar `sp_inicializar_preferencias_todos()` para usuarios existentes
- [ ] Verificar estadísticas con queries de verificación
- [ ] Documentar en manual de sistema

---

**¿Preguntas?** Revisa los comentarios en el script SQL o consulta la documentación del service Java.
