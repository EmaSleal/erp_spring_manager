## ✅ TESTING

### 1. Testing Manual con Endpoint
```bash
# Ejecutar desde Postman o curl
POST http://localhost:9090/configuracion/ejecutar-recordatorios
Authorization: Cookie (usuario ADMIN logueado)
```

### 2. Testing con Base de Datos
```sql
-- Crear una factura con pago vencido para testing
UPDATE factura 
SET fecha_pago = DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    entregado = true,
    tipo_factura = 'PENDIENTE'
WHERE id_factura = 1;

-- Verificar cliente tiene email
SELECT f.numero_factura, c.nombre, c.email, f.fecha_pago, f.tipo_factura
FROM factura f
JOIN cliente c ON f.id_cliente = c.id_cliente
WHERE f.fecha_pago < CURDATE()
  AND f.entregado = true
  AND f.tipo_factura = 'PENDIENTE';
```

### 3. Verificar Logs
- Revisar consola de Spring Boot
- Buscar líneas con "⏰" o "📧"
- Verificar cantidad de emails enviados

### 4. Verificar Recepción de Email
- Revisar bandeja de entrada del cliente
- Verificar datos de la factura
- Verificar cálculo de días de retraso

---

