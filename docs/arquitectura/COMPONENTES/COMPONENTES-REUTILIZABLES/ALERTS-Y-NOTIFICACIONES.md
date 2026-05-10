##  ALERTS Y NOTIFICACIONES

###  Ubicación
```
static/js/common.js (AppUtils.showToast)
SweetAlert2 (CDN)
```

###  Propósito
Notificaciones para:
- Operaciones exitosas
- Errores
- Confirmaciones
- Información

###  Uso

#### **Toast (notificación rápida):**
```javascript
// Éxito
AppUtils.showToast('Cliente guardado correctamente', 'success');

// Error
AppUtils.showToast('Error al guardar', 'error');

// Info
AppUtils.showToast('Procesando...', 'info');

// Warning
AppUtils.showToast('Revise los datos', 'warning');
```

#### **Confirmación (SweetAlert2):**
```javascript
AppUtils.showConfirmDialog(
    '¿Eliminar cliente?',
    'Esta acción no se puede deshacer',
    'warning'
).then((result) => {
    if (result.isConfirmed) {
        // Usuario confirmó
        eliminarCliente(id);
    }
});
```

#### **Alert Bootstrap:**
```html
<!-- Alert con auto-hide -->
<div class="alert alert-success alert-dismissible fade show" role="alert">
    <i class="fas fa-check-circle me-2"></i>
    ¡Operación exitosa!
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>

<script>
    // Auto-hide después de 5 segundos
    setTimeout(() => {
        const alert = document.querySelector('.alert');
        if (alert) {
            bootstrap.Alert.getOrCreateInstance(alert).close();
        }
    }, 5000);
</script>
```

###  Tipos de Alert

| Tipo | Clase | Color | Icono |
|------|-------|-------|-------|
| Éxito | `alert-success` | Verde | `fa-check-circle` |
| Error | `alert-danger` | Rojo | `fa-exclamation-circle` |
| Advertencia | `alert-warning` | Amarillo | `fa-exclamation-triangle` |
| Info | `alert-info` | Azul | `fa-info-circle` |

---

