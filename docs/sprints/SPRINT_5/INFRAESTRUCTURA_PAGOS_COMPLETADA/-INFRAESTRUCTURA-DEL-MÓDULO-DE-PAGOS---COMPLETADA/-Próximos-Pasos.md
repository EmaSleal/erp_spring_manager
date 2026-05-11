## 🚀 Próximos Pasos

### 1. Ejecutar Migración
```powershell
# Opción 1: MySQL Workbench
# Abrir y ejecutar: EJECUTAR_MIGRACION_PAGOS.sql

# Opción 2: Línea de comandos
mysql -u root -p whats_orders_manager < "docs\base de datos\EJECUTAR_MIGRACION_PAGOS.sql"
```

### 2. Verificar en Base de Datos
```sql
-- Verificar tabla
DESCRIBE pagos;

-- Verificar permisos
SELECT * FROM permiso WHERE categoria = 'Pagos';

-- Ver asignación por rol
SELECT r.nombre, p.codigo
FROM rol r
JOIN rol_permiso rp ON r.id_rol = rp.id_rol
JOIN permiso p ON rp.id_permiso = p.id_permiso
WHERE p.categoria = 'Pagos'
ORDER BY r.nombre, p.codigo;
```

### 3. Reiniciar Aplicación Spring Boot
```powershell
# Detener aplicación actual
# Limpiar y compilar
./mvnw clean compile

# Ejecutar
./mvnw spring-boot:run
```

### 4. Probar Endpoints REST
```bash
# Listar pagos
GET http://localhost:9090/pagos/api

# Crear pago
POST http://localhost:9090/pagos/api
Content-Type: application/json

{
  "clienteId": 1,
  "facturaId": 1,
  "monto": 50000.00,
  "fechaPago": "2026-01-18",
  "metodoPago": "EFECTIVO",
  "tipoPago": "TOTAL",
  "creadoPor": "ADMIN"
}
```

### 5. Crear Vistas Frontend
- Comenzar con `listar.html`
- Implementar `form.html` con validaciones
- Crear `detalle.html` con auditoría
- Desarrollar `estado-cuenta.html` para reportes

---

