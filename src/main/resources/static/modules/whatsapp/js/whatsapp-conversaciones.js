/**
 * whatsapp-conversaciones.js
 * Manejo de la vista de conversaciones WhatsApp
 */

/**
 * Filtra las conversaciones según el texto de búsqueda y checkbox de no leídos
 */
function filtrarConversaciones() {
    const buscar = document.getElementById('buscarTelefono').value.toLowerCase();
    const soloNoLeidos = document.getElementById('soloNoLeidos').checked;
    const conversaciones = document.querySelectorAll('.conversacion-item');
    
    let visibles = 0;
    
    conversaciones.forEach(conv => {
        const telefono = conv.getAttribute('data-telefono').toLowerCase();
        const nombre = conv.getAttribute('data-nombre').toLowerCase();
        const noLeidos = parseInt(conv.getAttribute('data-noleidos'));
        
        // Filtro de búsqueda
        const coincideBusqueda = telefono.includes(buscar) || nombre.includes(buscar);
        
        // Filtro de no leídos
        const cumpleNoLeidos = !soloNoLeidos || noLeidos > 0;
        
        // Mostrar/ocultar
        if (coincideBusqueda && cumpleNoLeidos) {
            conv.style.display = '';
            visibles++;
        } else {
            conv.style.display = 'none';
        }
    });
    
    console.log(`Mostrando ${visibles} conversaciones`);
}

/**
 * Abre el detalle de una conversación específica
 */
function verConversacion(telefono) {
    console.log('Abriendo conversación:', telefono);
    window.location.href = `/whatsapp/conversacion/${encodeURIComponent(telefono)}`;
}

/**
 * Muestra una alerta temporal
 */
function mostrarAlerta(mensaje, tipo = 'info') {
    const alertHtml = `
        <div class="alert alert-${tipo} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3" 
             style="z-index: 9999; min-width: 300px;" role="alert">
            ${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', alertHtml);
    
    // Auto-remover después de 5 segundos
    setTimeout(() => {
        const alert = document.querySelector('.alert');
        if (alert) {
            alert.remove();
        }
    }, 5000);
}

// Event listener para hover en conversaciones
document.addEventListener('DOMContentLoaded', function() {
    const conversaciones = document.querySelectorAll('.conversacion-item');
    
    conversaciones.forEach(conv => {
        // Hover effects
        conv.addEventListener('mouseenter', function() {
            this.style.backgroundColor = '#f8f9fa';
        });
        
        conv.addEventListener('mouseleave', function() {
            this.style.backgroundColor = '';
        });
        
        // Click handler
        conv.addEventListener('click', function() {
            const telefono = this.getAttribute('data-telefono');
            if (telefono) {
                verConversacion(telefono);
            }
        });
    });
});
