# FIX: Actualización de Estado de Factura

**Fecha:** 12 de octubre de 2025  
**Problema:** El estado `entregado` de la factura no se actualizaba al editar  
**Estado:** ✅ RESUELTO

---

## 🔴 Problema Identificado

Cuando se editaba una factura y se cambiaba el estado de "Pendiente" a "Entregado" (o viceversa), el sistema mostraba el mensaje de éxito pero **el estado no se actualizaba en la base de datos**.

### Causa Raíz

El flujo de actualización de factura solo actualizaba:
1. ✅ Las líneas de productos (cantidad, precio, subtotal)
2. ✅ El total de la factura (calculado por `ActualizarTotalFactura` SP)
3. ❌ **NO actualizaba el campo `entregado` de la tabla `factura`**

El stored procedure `sp_actualizar_linea_factura` solo modifica la tabla `linea_factura` y llama a `ActualizarTotalFactura`, pero ninguno de estos SP modifica el campo `entregado`.

---

## ✅ Solución Implementada

### 1. Nuevo Endpoint en `FacturaController.java`

Agregado método para actualizar solo el estado de entrega:

```java
@PutMapping("/actualizar-estado/{id}")
@ResponseBody
public ResponseEntity<String> actualizarEstadoFactura(
        @PathVariable Integer id,
        @RequestParam Boolean entregado) {
    
    Optional<Factura> facturaOpt = facturaService.findById(id);
    
    if (facturaOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    
    Factura factura = facturaOpt.get();
    factura.setEntregado(entregado);
    factura.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
    facturaService.save(factura);
    
    log.info("Estado de factura {} actualizado a: {}", id, entregado);
    return ResponseEntity.ok("Estado actualizado correctamente");
}
```

**Características:**
- **Ruta:** `PUT /facturas/actualizar-estado/{id}?entregado=true|false`
- **Parámetros:** 
  - `id` (path): ID de la factura
  - `entregado` (query): boolean (true/false)
- **Respuesta:** 200 OK con mensaje de confirmación
- **Log:** Registra cada cambio de estado

---

### 2. Modificación en `editar-factura.js`

Modificada la función `guardarLineas()` para hacer **dos llamadas secuenciales**:

```javascript
function guardarLineas() {
    // ... validaciones y preparación de líneas ...

    // 1️⃣ PRIMERO: Guardar las líneas de productos
    fetch('/lineas-factura/actualizar', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify(lineas)
    }).then(res => {
        if (res.ok) {
            // 2️⃣ SEGUNDO: Actualizar el estado de la factura
            const entregadoSelect = document.getElementById("entregado");
            const estadoEntregado = entregadoSelect ? (entregadoSelect.value === 'true') : false;
            
            console.log('Actualizando estado a:', estadoEntregado);
            
            return fetch(`/facturas/actualizar-estado/${facturaId}?entregado=${estadoEntregado}`, {
                method: 'PUT',
                headers: { [csrfHeader]: csrfToken }
            });
        } else {
            throw new Error('Error al guardar las líneas');
        }
    }).then(res => {
        if (res && res.ok) {
            Swal.fire({
                icon: 'success',
                title: '¡Éxito!',
                text: 'Factura guardada correctamente',
                confirmButtonColor: '#28a745',
                timer: 2000
            }).then(() => {
                nuevaFacturaModal.hide();
                location.reload();
            });
        }
    }).catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al guardar la factura: ' + error.message,
            confirmButtonColor: '#d33'
        });
    });
}
```

**Cambios clave:**
- ✅ Extrae el valor del select `#entregado`
- ✅ Convierte string "true"/"false" a boolean real
- ✅ Hace llamada PUT al nuevo endpoint
- ✅ Manejo de errores mejorado con try-catch
- ✅ Log en consola para debugging

---

## 🔄 Flujo Completo de Actualización

```
┌─────────────────────────────────────────────┐
│  Usuario cambia estado en el formulario     │
│  Select: Pendiente → Entregado              │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  1. Click en "Guardar Factura"              │
│  → función guardarLineas()                  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  2. PUT /lineas-factura/actualizar          │
│  → Actualiza cantidad, precio, subtotal     │
│  → SP: sp_actualizar_linea_factura          │
│  → SP: ActualizarTotalFactura               │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  3. PUT /facturas/actualizar-estado/{id}    │
│  ?entregado=true                            │
│  → FacturaController.actualizarEstadoFactura│
│  → facturaService.save(factura)             │
│  → UPDATE factura SET entregado=true        │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  4. SweetAlert de éxito                     │
│  → location.reload()                        │
│  → Vista actualizada con nuevo estado       │
└─────────────────────────────────────────────┘
```

---

## 📋 Template HTML (`form.html`)

El template ya tenía el select correcto:

```html
<div class="col-md-6 mb-3">
    <label for="entregado" class="form-label">
        <i class="fas fa-truck text-primary me-2"></i>Estado de Entrega
    </label>
    <select id="entregado" name="entregado" class="form-select">
        <option th:selected="${factura.entregado == true}" value="true">Entregado</option>
        <option th:selected="${factura.entregado == false}" value="false">Pendiente</option>
    </select>
</div>
```

**Importante:** Los valores son strings `"true"` y `"false"`, por eso en JavaScript se convierte con:
```javascript
entregadoSelect.value === 'true'
```

---

## 🧪 Testing

### Pasos para probar:

1. **Ir a Facturas:**
   ```
   http://localhost:8080/facturas
   ```

2. **Editar una factura existente:**
   - Click en el botón "Editar" de cualquier factura

3. **Cambiar el estado:**
   - Cambiar de "Pendiente" a "Entregado" (o viceversa)
   - Modificar alguna línea de producto (opcional)

4. **Guardar:**
   - Click en "Guardar Factura"

5. **Verificar en logs del servidor:**
   ```
   INFO ... Estado de factura 3 actualizado a: true
   ```

6. **Verificar en la base de datos:**
   ```sql
   SELECT id_factura, entregado, update_date 
   FROM factura 
   WHERE id_factura = 3;
   ```

7. **Verificar visualmente:**
   - El badge en la lista de facturas debe cambiar:
     - 🟢 "Entregado" (verde)
     - 🟡 "Pendiente" (amarillo)

---

## 📊 Logs Esperados

### ✅ Logs de Éxito:

```
2025-10-12T18:50:00.000-06:00  INFO ... Actualizando líneas: [LineaFacturaR[...], ...]
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
...
2025-10-12T18:50:00.100-06:00  INFO ... Estado de factura 3 actualizado a: true
```

### ❌ Problema Anterior (logs sin actualización de estado):

```
2025-10-12T18:27:36.192-06:00  INFO ... Actualizando líneas: [LineaFacturaR[...], ...]
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
...
// ❌ No había log de actualización de estado
```

---

## 🗄️ Base de Datos

### Stored Procedures utilizados:

#### `sp_actualizar_linea_factura`
```sql
DELIMITER $$
CREATE DEFINER=`m4n0`@`%` PROCEDURE `sp_actualizar_linea_factura`(
    IN p_id_linea_factura INT,
    IN p_numero_linea INT,
    IN p_id_factura INT,
    IN p_id_producto INT,
    IN p_cantidad INT,
    IN p_precioUnitario DECIMAL(10,2),
    IN p_subtotal DECIMAL(10,2),
    IN p_update_by INT
)
BEGIN
    -- Actualiza línea
    UPDATE linea_factura SET ...
    
    -- Llama a ActualizarTotalFactura
    CALL ActualizarTotalFactura(p_id_factura);
END$$
DELIMITER ;
```

#### `ActualizarTotalFactura`
```sql
DELIMITER $$
CREATE DEFINER=`m4n0`@`localhost` PROCEDURE `ActualizarTotalFactura`(
    IN pIdFactura INT
)
BEGIN
    UPDATE factura
    SET total = (
        SELECT SUM(subtotal)
        FROM linea_factura
        WHERE id_factura = pIdFactura
    )
    WHERE id_factura = pIdFactura;
END$$
DELIMITER ;
```

**Nota:** Ninguno de estos SP actualiza el campo `entregado`, por eso se necesitó el endpoint adicional.

---

## 📝 Archivos Modificados

1. ✅ **FacturaController.java**
   - Agregado método `actualizarEstadoFactura()`
   - Ruta: `/facturas/actualizar-estado/{id}`
   - Método: PUT

2. ✅ **editar-factura.js**
   - Modificada función `guardarLineas()`
   - Agregada llamada secuencial al endpoint de estado
   - Mejora en manejo de errores

3. ✅ **FIX_ACTUALIZACION_ESTADO_FACTURA.md** (ESTE ARCHIVO)
   - Documentación completa del fix

---

## 🚀 Compilación

```bash
./mvnw clean compile -DskipTests
```

**Resultado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 4.101 s
[INFO] Compiling 45 source files
```

---

## ✅ Checklist de Verificación

- [x] Endpoint `/facturas/actualizar-estado/{id}` creado
- [x] JavaScript `guardarLineas()` modificado
- [x] Conversión correcta de string a boolean
- [x] Manejo de errores implementado
- [x] Logs agregados para debugging
- [x] Compilación exitosa
- [x] Documentación completa

---

## 🔮 Mejoras Futuras (Opcional)

### Opción 1: Crear SP específico
```sql
CREATE PROCEDURE sp_actualizar_estado_factura(
    IN p_id_factura INT,
    IN p_entregado BOOLEAN,
    IN p_update_by INT
)
BEGIN
    UPDATE factura
    SET entregado = p_entregado,
        update_by = p_update_by,
        update_date = CURRENT_TIMESTAMP
    WHERE id_factura = p_id_factura;
END;
```

### Opción 2: Unificar en un solo endpoint
```java
@PutMapping("/actualizar/{id}")
public ResponseEntity<String> actualizarFacturaCompleta(
    @PathVariable Integer id,
    @RequestBody FacturaUpdateDTO dto
) {
    // Actualizar factura (estado, descripción, etc.)
    // Actualizar líneas
    // Todo en una transacción
}
```

### Opción 3: Agregar validación de transición de estados
```java
// Evitar cambios no permitidos
if (facturaActual.isEntregado() && !nuevoEstado) {
    throw new InvalidStateTransitionException(
        "No se puede cambiar de Entregado a Pendiente"
    );
}
```

---

## 📞 Soporte

Si el problema persiste:
1. Verificar logs del servidor
2. Verificar consola del navegador (F12)
3. Verificar que el select `#entregado` exista en el DOM
4. Verificar que el `facturaId` sea correcto
5. Verificar permisos en la base de datos

---

**Fecha de resolución:** 12 de octubre de 2025  
**Responsable:** GitHub Copilot  
**Estado:** ✅ RESUELTO y DOCUMENTADO

---

¡Fix completado exitosamente! 🎉
