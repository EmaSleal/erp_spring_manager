# 📝 Guía de Inicialización de Preferencias de Notificación

## Problema Detectado
La tabla `preferencia_notificacion` está vacía y los usuarios no tienen configuradas sus preferencias de notificaciones.

## Soluciones Disponibles

### ✅ Opción 1: Endpoint REST (Recomendado)

#### Para Usuarios Individuales
Cada usuario puede inicializar sus propias preferencias:

**Endpoint:** `POST /api/notificaciones/preferencias/inicializar`

**cURL:**
```bash
curl -X POST http://localhost:8080/api/notificaciones/preferencias/inicializar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Preferencias inicializadas correctamente",
  "totalPreferencias": 5,
  "preferencias": [...]
}
```

**Respuesta si ya tiene preferencias:**
```json
{
  "success": false,
  "message": "El usuario ya tiene preferencias configuradas",
  "totalPreferencias": 5
}
```

---

#### Para Todos los Usuarios (ADMIN)
El administrador puede inicializar preferencias para todos los usuarios de una sola vez:

**Endpoint:** `POST /api/notificaciones/admin/inicializar-preferencias`

**Requisito:** Usuario con rol ADMIN

**cURL:**
```bash
curl -X POST http://localhost:8080/api/notificaciones/admin/inicializar-preferencias \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Inicialización completada exitosamente",
  "totalUsuarios": 5,
  "usuariosConPreferenciasExistentes": 1,
  "usuariosSinPreferencias": 4,
  "preferenciasCreadas": 20
}
```

---

### ✅ Opción 2: Script SQL

Si prefieres ejecutar SQL directamente en tu base de datos:

**Archivo:** `docs/base de datos/INIT_PREFERENCIAS_NOTIFICACION.sql`

**Pasos:**

1. Abrir el archivo SQL
2. Ejecutar el script completo en tu gestor de base de datos (DBeaver, MySQL Workbench, etc.)
3. Verificar los resultados con las consultas de verificación al final del script

**El script realiza:**

- ✅ Verifica usuarios sin preferencias
- ✅ Crea preferencia GLOBAL para cada usuario
- ✅ Crea preferencias para FACTURA_VENCIDA (WEB y EMAIL)
- ✅ Crea preferencias para STOCK_BAJO (WEB y EMAIL)
- ✅ Muestra estadísticas y resumen

---

## Preferencias Predeterminadas Creadas

### 1. Preferencia Global
```
tipo_notificacion: NULL (aplica para todos)
canal: NULL (aplica para todos)
activa: true
frecuencia: INMEDIATA
```

### 2. Facturas Vencidas
```
tipo_notificacion: FACTURA_VENCIDA
canal: WEB
activa: true
frecuencia: INMEDIATA
```
```
tipo_notificacion: FACTURA_VENCIDA
canal: EMAIL
activa: true
frecuencia: INMEDIATA
```

### 3. Stock Bajo
```
tipo_notificacion: STOCK_BAJO
canal: WEB
activa: true
frecuencia: INMEDIATA
```
```
tipo_notificacion: STOCK_BAJO
canal: EMAIL
activa: true
frecuencia: DIARIA (resumen diario)
```

---

## Cómo Probar en el Sistema

### 1. Desde Postman/Insomnia

**Request:**
```
POST http://localhost:8080/api/notificaciones/admin/inicializar-preferencias
```

**Headers:**
```
Content-Type: application/json
```

**Body:** (vacío)

---

### 2. Desde JavaScript en el Navegador

```javascript
fetch('/api/notificaciones/admin/inicializar-preferencias', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    }
})
.then(response => response.json())
.then(data => {
    console.log('Resultado:', data);
    alert(`✅ Preferencias creadas: ${data.preferenciasCreadas} para ${data.usuariosSinPreferencias} usuarios`);
})
.catch(error => console.error('Error:', error));
```

---

### 3. Desde cURL (Terminal)

```bash
# Inicializar para todos los usuarios (requiere ser admin)
curl -X POST http://localhost:8080/api/notificaciones/admin/inicializar-preferencias \
  -H "Content-Type: application/json"

# Inicializar solo para usuario actual
curl -X POST http://localhost:8080/api/notificaciones/preferencias/inicializar \
  -H "Content-Type: application/json"
```

---

## Verificación

### Consulta SQL para verificar las preferencias creadas:

```sql
-- Ver todas las preferencias
SELECT 
    u.nombre as usuario,
    pn.tipo_notificacion,
    pn.canal,
    pn.activa,
    pn.frecuencia
FROM usuario u
INNER JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
ORDER BY u.nombre, pn.tipo_notificacion, pn.canal;

-- Contar preferencias por usuario
SELECT 
    u.nombre,
    COUNT(pn.id_preferencia) as total_preferencias
FROM usuario u
LEFT JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
GROUP BY u.id_usuario, u.nombre;
```

---

## Comportamiento del Sistema

### Lógica de Preferencias

1. **Preferencia Global (NULL, NULL):**
   - Si `activa = true`: Usuario RECIBE todas las notificaciones
   - Si `activa = false`: Usuario NO recibe ninguna notificación
   - Tiene prioridad sobre preferencias específicas

2. **Preferencias Específicas:**
   - Se evalúan solo si la preferencia global lo permite
   - Permiten control granular por tipo y canal
   - Pueden tener diferentes frecuencias (INMEDIATA, DIARIA, SEMANAL)

3. **Campo `notificacionesDesactivadasGlobal`:**
   - Si es `true`: Usuario NO recibe NINGUNA notificación
   - Tiene máxima prioridad sobre todas las demás configuraciones

---

## Logs Esperados

Al ejecutar la inicialización, verás en los logs:

```
🔧 Iniciando inicialización de preferencias para todos los usuarios
📝 Creando preferencias para usuario: Juan Pérez (ID: 1)
📝 Creando preferencias para usuario: María García (ID: 2)
✅ Inicialización completada: 20 preferencias creadas para 4 usuarios
```

---

## Troubleshooting

### Error: "Solo administradores pueden ejecutar esta acción"
**Causa:** El usuario no tiene rol ADMIN  
**Solución:** Usar el endpoint individual `/preferencias/inicializar` o autenticarse como admin

### Error: "El usuario ya tiene preferencias configuradas"
**Causa:** El usuario ya tiene preferencias en la BD  
**Solución:** Esto es normal, no se crean preferencias duplicadas

### Sin registros creados
**Causa:** Todos los usuarios ya tienen preferencias  
**Solución:** Verificar con la consulta SQL de verificación

---

## Próximos Pasos

Una vez inicializadas las preferencias:

1. ✅ Los usuarios pueden ver sus notificaciones
2. ✅ El sistema puede enviar notificaciones según preferencias
3. ✅ Los usuarios pueden modificar sus preferencias desde la UI
4. ✅ Las notificaciones se filtran automáticamente

---

## Contacto y Soporte

Si tienes problemas:
1. Revisa los logs del servidor
2. Verifica que la tabla `preferencia_notificacion` existe
3. Confirma que hay usuarios activos en la tabla `usuario`
4. Ejecuta las consultas SQL de verificación
