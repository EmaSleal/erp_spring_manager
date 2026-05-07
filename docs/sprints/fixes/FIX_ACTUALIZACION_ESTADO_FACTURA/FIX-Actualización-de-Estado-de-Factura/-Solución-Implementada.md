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

