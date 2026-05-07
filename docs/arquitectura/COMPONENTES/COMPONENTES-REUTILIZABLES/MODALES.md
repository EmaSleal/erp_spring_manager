##  MODALES

###  Ubicación
```
Bootstrap 5 nativo
```

###  Propósito
Diálogos flotantes para:
- Formularios de agregar/editar
- Confirmaciones
- Información adicional

###  Uso

```html
<!-- Trigger Button -->
<button 
    type="button" 
    class="btn btn-primary" 
    data-bs-toggle="modal" 
    data-bs-target="#miModal">
    <i class="fas fa-plus me-1"></i>Agregar
</button>

<!-- Modal -->
<div class="modal fade" id="miModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <!-- Header -->
            <div class="modal-header">
                <h5 class="modal-title">
                    <i class="fas fa-plus me-2"></i>Agregar Cliente
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            
            <!-- Body -->
            <div class="modal-body">
                <form id="formModal">
                    <!-- Campos del formulario -->
                </form>
            </div>
            
            <!-- Footer -->
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    Cancelar
                </button>
                <button type="button" class="btn btn-primary" onclick="guardar()">
                    Guardar
                </button>
            </div>
        </div>
    </div>
</div>
```

###  JavaScript

```javascript
// Abrir modal programáticamente
const modal = new bootstrap.Modal(document.getElementById('miModal'));
modal.show();

// Cerrar modal
modal.hide();

// Llenar modal con datos (editar)
function editarCliente(id) {
    fetch(`/api/clientes/${id}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('nombre').value = data.nombre;
            document.getElementById('email').value = data.email;
            // ... otros campos
            
            modal.show();
        });
}
```

---

