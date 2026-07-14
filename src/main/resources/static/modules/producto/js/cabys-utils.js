// ========================================
// Utilidades para búsqueda de CABYS
// Facturación Electrónica Costa Rica
// ========================================

/**
 * Buscar códigos CABYS en la API de Hacienda
 */
function buscarCabys() {
    const termino = document.getElementById('cabysBusqueda').value.trim();
    
    if (!termino) {
        showToast('warning', 'Por favor ingrese un término de búsqueda');
        return;
    }
    
    // Mostrar loading
    const btnBuscar = document.getElementById('btnBuscarCabys');
    const textoOriginal = btnBuscar.innerHTML;
    btnBuscar.disabled = true;
    btnBuscar.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Buscando...';

    // Llamar al endpoint
    httpGet(`/api/configuracion/empresa/hacienda/cabys/buscar?q=${encodeURIComponent(termino)}&top=10`, {
        showLoading: false,
        onSuccess: (data) => {
            btnBuscar.disabled = false;
            btnBuscar.innerHTML = textoOriginal;

            if (data.data && data.data.cabys) {
                mostrarResultadosCabys(data.data.cabys);
            } else {
                showToast('info', data.message || 'No se encontraron códigos CABYS para el término buscado');
            }
        },
        onError: () => {
            btnBuscar.disabled = false;
            btnBuscar.innerHTML = textoOriginal;
            showToast('error', 'No se pudo conectar con la API de Hacienda. Intente nuevamente.');
        }
    });
}

/**
 * Mostrar resultados de búsqueda CABYS
 */
function mostrarResultadosCabys(cabys) {
    const divResultados = document.getElementById('cabysResultados');
    const listGroup = document.getElementById('cabysListGroup');
    
    // Limpiar resultados anteriores
    listGroup.innerHTML = '';
    
    if (!cabys || cabys.length === 0) {
        divResultados.style.display = 'none';
        return;
    }
    
    // Crear elementos de la lista
    cabys.forEach(item => {
        const elemento = document.createElement('a');
        elemento.href = '#';
        elemento.className = 'list-group-item list-group-item-action';
        elemento.onclick = (e) => {
            e.preventDefault();
            seleccionarCabys(item);
        };
        
        elemento.innerHTML = `
            <div class="d-flex w-100 justify-content-between">
                <h6 class="mb-1">
                    <span class="badge bg-warning text-dark">${item.codigo}</span>
                    ${item.descripcion}
                </h6>
                ${item.impuesto ? `<small class="text-muted">${item.impuesto}% IVA</small>` : ''}
            </div>
            ${item.categorias ? `<small class="text-muted">${item.categorias}</small>` : ''}
        `;
        
        listGroup.appendChild(elemento);
    });
    
    // Mostrar contenedor de resultados
    divResultados.style.display = 'block';
    
    // Toast informativo
    showToast('success', `Se encontraron ${cabys.length} códigos CABYS`);
}

/**
 * Seleccionar un código CABYS y llenar los campos
 */
function seleccionarCabys(cabys) {
    // Llenar campos
    document.getElementById('codigoCabys').value = cabys.codigo;
    document.getElementById('descripcionCabys').value = cabys.descripcion;
    
    // Si viene el porcentaje de impuesto, actualizar
    if (cabys.impuesto) {
        document.getElementById('porcentajeImpuesto').value = cabys.impuesto;
        document.getElementById('gravado').checked = cabys.impuesto > 0;
        toggleImpuesto(); // Actualizar visibilidad
    }
    
    // Ocultar resultados
    document.getElementById('cabysResultados').style.display = 'none';
    
    // Limpiar búsqueda
    document.getElementById('cabysBusqueda').value = '';
    
    // Toast de confirmación
    showToast('success', 'Código CABYS seleccionado');
}

/**
 * Mostrar/ocultar campo de porcentaje de impuesto según checkbox gravado
 */
function toggleImpuesto() {
    const gravado = document.getElementById('gravado');
    const divPorcentaje = document.getElementById('divPorcentajeImpuesto');
    
    if (!gravado || !divPorcentaje) return;
    
    if (gravado.checked) {
        divPorcentaje.style.display = 'block';
        // Si está vacío, poner 13% por defecto
        const porcentaje = document.getElementById('porcentajeImpuesto');
        if (!porcentaje.value || porcentaje.value === '0') {
            porcentaje.value = '13.00';
        }
    } else {
        divPorcentaje.style.display = 'none';
        document.getElementById('porcentajeImpuesto').value = '0';
    }
}

// Permitir búsqueda con Enter
document.addEventListener('DOMContentLoaded', () => {
    const cabysBusqueda = document.getElementById('cabysBusqueda');
    if (cabysBusqueda) {
        cabysBusqueda.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarCabys();
            }
        });
    }
    
    // Inicializar estado del toggle de impuesto al cargar
    const gravado = document.getElementById('gravado');
    if (gravado) {
        toggleImpuesto();
        gravado.addEventListener('change', toggleImpuesto);
    }
});
