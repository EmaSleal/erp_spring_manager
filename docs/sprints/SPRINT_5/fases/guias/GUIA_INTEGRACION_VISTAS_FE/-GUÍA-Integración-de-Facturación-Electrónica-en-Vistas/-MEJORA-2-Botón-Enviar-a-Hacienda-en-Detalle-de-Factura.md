## 🔘 MEJORA 2: Botón "Enviar a Hacienda" en Detalle de Factura

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/facturas.html` (Modal de detalle)

### Implementación

#### 2.1 Agregar Botón en Card de Acciones

```html
<!-- En el modal de detalle de factura -->
<div class="modal-footer">
    <div class="btn-group" role="group">
        <!-- Botones existentes -->
        <button type="button" class="btn btn-primary" onclick="imprimirFactura(${factura.id})">
            <i class="bi bi-printer"></i> Imprimir
        </button>
        
        <button type="button" class="btn btn-info" onclick="enviarEmail(${factura.id})">
            <i class="bi bi-envelope"></i> Email
        </button>
        
        <!-- NUEVO BOTÓN: Enviar a Hacienda -->
        <button th:if="${!factura.comprobanteElectronico}" 
                type="button" 
                class="btn btn-success"
                onclick="enviarAHacienda([[${factura.id}]])"
                title="Enviar factura a Hacienda de Costa Rica">
            <i class="bi bi-cloud-upload"></i> Enviar a Hacienda
        </button>
        
        <!-- Si ya tiene comprobante, mostrar botón de estado -->
        <button th:if="${factura.comprobanteElectronico}" 
                type="button" 
                class="btn btn-outline-info"
                onclick="verComprobante([[${factura.comprobanteElectronico.id}]])"
                title="Ver comprobante electrónico">
            <i class="bi bi-file-earmark-check"></i> Ver Comprobante
        </button>
    </div>
    
    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
</div>
```

#### 2.2 JavaScript para Envío a Hacienda

```javascript
/**
 * Envía una factura a Hacienda de Costa Rica.
 */
async function enviarAHacienda(facturaId) {
    // Mostrar modal de confirmación
    const confirmed = await Swal.fire({
        title: '¿Enviar a Hacienda?',
        html: `
            <p>Se generará y enviará el comprobante electrónico a Hacienda de Costa Rica.</p>
            <div class="alert alert-info mt-3">
                <strong>Información:</strong>
                <ul class="text-start mb-0">
                    <li>Se generará el XML según especificación v4.4</li>
                    <li>Se firmará digitalmente con certificado</li>
                    <li>Se enviará a la API de Hacienda</li>
                    <li>Recibirá notificación del resultado</li>
                </ul>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-cloud-upload"></i> Enviar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#198754',
        showLoaderOnConfirm: true,
        preConfirm: async () => {
            try {
                const response = await fetch(`/api/facturas/electronica/comprobantes`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        facturaId: facturaId
                    })
                });
                
                if (!response.ok) {
                    const error = await response.json();
                    throw new Error(error.message || 'Error al enviar comprobante');
                }
                
                return await response.json();
                
            } catch (error) {
                Swal.showValidationMessage(`Error: ${error.message}`);
            }
        },
        allowOutsideClick: () => !Swal.isLoading()
    });
    
    if (confirmed.isConfirmed) {
        const comprobante = confirmed.value;
        
        // Mostrar resultado
        await Swal.fire({
            title: '¡Enviado!',
            html: `
                <div class="alert alert-success">
                    <h6>Comprobante enviado exitosamente</h6>
                    <p class="mb-1"><strong>Clave:</strong> ${comprobante.claveNumerica}</p>
                    <p class="mb-1"><strong>Estado:</strong> ${comprobante.estado}</p>
                </div>
                <p>Será redirigido a la vista de comprobantes electrónicos...</p>
            `,
            icon: 'success',
            timer: 3000,
            timerProgressBar: true
        });
        
        // Redirigir a comprobantes
        window.location.href = `/facturas/comprobantes?id=${comprobante.id}`;
    }
}

/**
 * Ver detalle de comprobante electrónico.
 */
function verComprobante(comprobanteId) {
    window.location.href = `/facturas/comprobantes?id=${comprobanteId}`;
}
```

---

