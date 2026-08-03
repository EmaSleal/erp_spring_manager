/**
 * JavaScript para gestión de cuentas contables
 * Sprint 5 - Fase 2: Contabilidad
 */

// Variables globales
let cuentas = [];
let cuentasFiltradas = [];

// ============================================
// INICIALIZACIÓN
// ============================================

document.addEventListener('DOMContentLoaded', function() {
    // Detectar la página actual
    const currentPath = window.location.pathname;
    
    if (currentPath.includes('/cuentas/crear') || currentPath.includes('/editar')) {
        initFormCuenta();
    } else if (currentPath.includes('/cuentas')) {
        initListaCuentas();
    }
});

// ============================================
// PÁGINA: LISTADO DE CUENTAS
// ============================================

function initListaCuentas() {
    cargarCuentas();
    
    // Event listeners para filtros
    document.getElementById('txtBuscar')?.addEventListener('input', debounce(aplicarFiltros, 300));
    document.getElementById('selTipo')?.addEventListener('change', aplicarFiltros);
    document.getElementById('selEstado')?.addEventListener('change', aplicarFiltros);
}

async function cargarCuentas() {
    try {
        showElement('loadingCuentas');
        hideElement('emptyCuentas');
        hideElement('treeCuentas');
        
        const response = await httpGet('/contabilidad/cuentas/api/raiz', {
            showLoading: false
        });
        
        hideElement('loadingCuentas');
        
        if (response && response.length > 0) {
            console.log('Cuentas cargadas:', response);
            cuentas = response;
            cuentasFiltradas = response;
            renderizarArbol();
        } else {
            showElement('emptyCuentas');
        }
    } catch (error) {
        hideElement('loadingCuentas');
        console.error('Error al cargar cuentas:', error);
        showToast('error', 'Error al cargar el plan de cuentas');
        showElement('emptyCuentas');
    }
}

function renderizarArbol() {
    const container = document.getElementById('treeCuentas');
    if (!container) return;
    
    container.innerHTML = '';
    
    if (cuentasFiltradas.length === 0) {
        container.innerHTML = `
            <div class="text-center py-4">
                <i class="fas fa-search fa-3x text-muted mb-3"></i>
                <h5 class="text-muted">No se encontraron cuentas</h5>
                <p class="text-muted">Intente con otros criterios de búsqueda</p>
            </div>
        `;
        showElement('treeCuentas');
        return;
    }
    
    // Crear estructura de árbol
    const tree = document.createElement('div');
    tree.className = 'cuenta-tree';
    
    cuentasFiltradas.forEach(cuenta => {
        tree.appendChild(crearNodoCuenta(cuenta, 0));
    });
    
    container.appendChild(tree);
    showElement('treeCuentas');
}

function crearNodoCuenta(cuenta, nivel) {
    const node = document.createElement('div');
    node.className = 'cuenta-node';
    node.style.marginLeft = `${nivel * 20}px`;
    
    const tieneSubcuentas = cuenta.subcuentas && cuenta.subcuentas.length > 0;
    const iconoTipo = getIconoTipo(cuenta.tipo);
    const colorTipo = getColorTipo(cuenta.tipo);
    
    node.innerHTML = `
        <div class="d-flex align-items-center justify-content-between py-2 px-3 cuenta-item ${!cuenta.activa ? 'cuenta-inactiva' : ''}"
             data-cuenta-id="${cuenta.idCuenta}">
            <div class="d-flex align-items-center flex-grow-1">
                ${tieneSubcuentas ? `
                    <button type="button" class="btn btn-sm btn-link text-muted p-0 me-2" 
                            onclick="toggleSubcuentas(${cuenta.idCuenta})">
                        <i class="fas fa-chevron-right" id="chevron-${cuenta.idCuenta}"></i>
                    </button>
                ` : '<span class="me-4"></span>'}
                
                <span class="badge bg-${colorTipo} me-2">${iconoTipo}</span>
                <strong class="me-2">${cuenta.codigo}</strong>
                <span>${cuenta.nombre}</span>

                ${!cuenta.activa ? '<span class="badge badge-secondary ms-2">Inactiva</span>' : ''}
                ${!cuenta.aceptaMovimientos ? '<span class="badge badge-outline-secondary ms-2">No acepta movimientos</span>' : ''}
                ${cuenta.tieneMovimientos ? '<span class="badge badge-secondary ms-2"><i class="fas fa-check"></i> Con movimientos</span>' : ''}
            </div>

            <div class="btn-group btn-group-sm">
                <a href="/contabilidad/cuentas/${cuenta.idCuenta}" class="btn btn-outline-secondary" title="Ver detalle">
                    <i class="fas fa-eye"></i>
                </a>
                <a href="/contabilidad/cuentas/crear?cuentaPadreId=${cuenta.idCuenta}"
                   class="btn btn-outline-secondary" title="Agregar subcuenta">
                    <i class="fas fa-plus"></i>
                </a>
                <a href="/contabilidad/cuentas/${cuenta.idCuenta}/editar"
                   class="btn btn-module-contabilidad" title="Editar">
                    <i class="fas fa-edit"></i>
                </a>
                <button type="button" class="btn btn-outline-danger" 
                        onclick="eliminarCuenta(${cuenta.idCuenta}, '${cuenta.codigo}')" 
                        title="Eliminar"
                        ${cuenta.tieneMovimientos || (cuenta.cantidadSubcuentas > 0) ? 'disabled' : ''}>
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
        
        ${tieneSubcuentas ? `
            <div id="subcuentas-${cuenta.idCuenta}" class="subcuentas-container d-none">
                ${cuenta.subcuentas.map(sub => crearNodoCuenta(sub, nivel + 1).outerHTML).join('')}
            </div>
        ` : ''}
    `;
    
    return node;
}

function getIconoTipo(tipo) {
    const iconos = {
        'ACTIVO': '<i class="fas fa-wallet"></i>',
        'PASIVO': '<i class="fas fa-file-invoice-dollar"></i>',
        'CAPITAL': '<i class="fas fa-university"></i>',
        'INGRESO': '<i class="fas fa-arrow-down"></i>',
        'EGRESO': '<i class="fas fa-arrow-up"></i>'
    };
    return iconos[tipo] || '<i class="fas fa-circle"></i>';
}

function getColorTipo(tipo) {
    const colores = {
        'ACTIVO': 'success',
        'PASIVO': 'danger',
        'CAPITAL': 'cuenta-capital',
        'INGRESO': 'info',
        'EGRESO': 'warning'
    };
    return colores[tipo] || 'secondary';
}

function toggleSubcuentas(cuentaId) {
    const container = document.getElementById(`subcuentas-${cuentaId}`);
    const chevron = document.getElementById(`chevron-${cuentaId}`);
    const isHidden = container.classList.contains('d-none');

    container.classList.toggle('d-none', !isHidden);
    chevron.classList.toggle('fa-chevron-right', !isHidden);
    chevron.classList.toggle('fa-chevron-down', isHidden);
}

function expandirTodo() {
    document.querySelectorAll('.subcuentas-container').forEach(c => c.classList.remove('d-none'));
    document.querySelectorAll('[id^="chevron-"]').forEach(chevron => {
        chevron.classList.remove('fa-chevron-right');
        chevron.classList.add('fa-chevron-down');
    });
}

function colapsarTodo() {
    document.querySelectorAll('.subcuentas-container').forEach(c => c.classList.add('d-none'));
    document.querySelectorAll('[id^="chevron-"]').forEach(chevron => {
        chevron.classList.remove('fa-chevron-down');
        chevron.classList.add('fa-chevron-right');
    });
}

async function aplicarFiltros() {
    const termino = document.getElementById('txtBuscar')?.value.toLowerCase() || '';
    const tipo = document.getElementById('selTipo')?.value || '';
    const estado = document.getElementById('selEstado')?.value || '';
    
    try {
        let url = '/contabilidad/cuentas/api/todas';
        
        // Si hay búsqueda por término, usar endpoint de búsqueda
        if (termino) {
            const response = await httpGet(`/contabilidad/cuentas/api/buscar?termino=${encodeURIComponent(termino)}`, {
                showLoading: false
            });
            cuentasFiltradas = response || [];
        } else if (tipo) {
            const response = await httpGet(`/contabilidad/cuentas/api/tipo/${tipo}`, {
                showLoading: false
            });
            cuentasFiltradas = response || [];
        } else {
            cuentasFiltradas = [...cuentas];
        }
        
        // Filtrar por estado
        if (estado !== '') {
            const esActiva = estado === 'true';
            cuentasFiltradas = cuentasFiltradas.filter(c => c.activa === esActiva);
        }
        
        renderizarArbol();
    } catch (error) {
        console.error('Error al filtrar cuentas:', error);
        showToast('error', 'Error al aplicar filtros');
    }
}

async function eliminarCuenta(id, codigo) {
    const confirmado = await showConfirmDialog(
        '¿Eliminar Cuenta?',
        `¿Está seguro de eliminar la cuenta ${codigo}? Esta acción no se puede deshacer.`,
        'warning'
    );
    
    if (!confirmado) return;
    
    await httpDelete(`/contabilidad/cuentas/api/${id}`, {
        successMessage: 'Cuenta eliminada exitosamente',
        onSuccess: () => {
            cargarCuentas();
        }
    });
}

// ============================================
// PÁGINA: FORMULARIO DE CUENTA
// ============================================

function initFormCuenta() {
    const form = document.getElementById('formCuenta');
    if (!form) return;
    
    // Event listener para el formulario
    form.addEventListener('submit', guardarCuenta);
    
    // Inicializar naturaleza según tipo
    actualizarNaturaleza();
}

function actualizarNaturaleza() {
    const tipo = document.getElementById('tipo')?.value;
    const naturaleza = document.getElementById('naturaleza');
    
    if (!naturaleza) return;
    
    const naturalezas = {
        'ACTIVO': 'DEUDORA',
        'EGRESO': 'DEUDORA',
        'PASIVO': 'ACREEDORA',
        'CAPITAL': 'ACREEDORA',
        'INGRESO': 'ACREEDORA'
    };
    
    naturaleza.value = naturalezas[tipo] || 'DEUDORA';
}

async function generarCodigo() {
    const cuentaPadreId = document.getElementById('cuentaPadreId')?.value;
    const codigoPadre = cuentaPadreId ? await obtenerCodigoPadre(cuentaPadreId) : '';
    
    try {
        const response = await httpGet(`/contabilidad/cuentas/api/siguiente-codigo?codigoPadre=${codigoPadre}`, {
            showLoading: false
        });
        
        if (response && response.codigo) {
            document.getElementById('codigo').value = response.codigo;
            showToast('success', 'Código generado automáticamente');
        }
    } catch (error) {
        console.error('Error al generar código:', error);
        showToast('error', 'Error al generar código');
    }
}

async function obtenerCodigoPadre(cuentaPadreId) {
    try {
        const response = await httpGet(`/contabilidad/cuentas/api/${cuentaPadreId}`, {
            showLoading: false
        });
        return response?.codigo || '';
    } catch (error) {
        console.error('Error al obtener cuenta padre:', error);
        return '';
    }
}

async function guardarCuenta(event) {
    event.preventDefault();
    
    const idCuenta = document.getElementById('idCuenta')?.value;
    const cuentaPadreId = document.getElementById('cuentaPadreId')?.value;
    
    const datos = {
        idCuenta: idCuenta || null,
        codigo: document.getElementById('codigo').value,
        nombre: document.getElementById('nombre').value,
        descripcion: document.getElementById('descripcion').value,
        tipo: document.getElementById('tipo').value,
        naturaleza: document.getElementById('naturaleza').value,
        activa: document.getElementById('activa').checked,
        aceptaMovimientos: document.getElementById('aceptaMovimientos').checked,
        cuentaPadreId: cuentaPadreId || null
    };
    
    const esEdicion = !!idCuenta;
    const url = esEdicion ? `/contabilidad/cuentas/api/${idCuenta}` : '/contabilidad/cuentas/api';
    const method = esEdicion ? httpPut : httpPost;
    
    await method(url, datos, {
        successMessage: esEdicion ? 'Cuenta actualizada exitosamente' : 'Cuenta creada exitosamente',
        redirectOnSuccess: '/contabilidad/cuentas'
    });
}

// ============================================
// UTILIDADES
// ============================================

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function showElement(id) {
    const element = document.getElementById(id);
    if (element) {
        element.classList.remove('d-none');
    }
}

function hideElement(id) {
    const element = document.getElementById(id);
    if (element) {
        element.classList.add('d-none');
    }
}
