## Soluciones Disponibles

### ✅ Opción 1: Endpoint REST (Recomendado)

#### Para Usuarios Individuales
Cada usuario puede inicializar sus propias preferencias:

**Endpoint:** `POST /api/notificaciones/preferencias/inicializar`

**cURL:**
```bash
curl -X POST http://localhost:9090/api/notificaciones/preferencias/inicializar \
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
curl -X POST http://localhost:9090/api/notificaciones/admin/inicializar-preferencias \
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

