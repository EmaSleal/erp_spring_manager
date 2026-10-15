// ============================================
// ASIENTOS CONTABLES - JAVASCRIPT
// ============================================

// Variables globales
let paginaActual = 0;
let totalPaginas = 0;
let cuentasDisponibles = [];
let detallesAsiento = [];
let contadorDetalle = 0;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    const currentPath = window.location.pathname;
    
    if (currentPath.includes('/asientos/crear') || currentPath.includes('/asientos') && currentPath.includes('/editar')) {
        initFormAsiento();
    } else if (currentPath === '/contabilidad/asientos' || currentPath === '/contabilidad/asientos/') {
        initListaAsientos();
    } else if (currentPath.includes('/asientos/') && !currentPath.includes('/editar')) {
        initDetalleAsiento();
    }
});

// ============================================
// PÁGINA: LISTADO DE ASIENTOS
// ============================================

function initListaAsientos() {
    cargarAsientos();
    
    // Event listeners
    document.getElementById('txtBuscar')?.addEventListener('input', debounce(aplicarFiltros, 300));
    document.getElementById('selTipo')?.addEventListener('change', aplicarFiltros);
    document.getElementById('selEstado')?.addEventListener('change', aplicarFiltros);
}

async function cargarAsientos(pagina = 0) {
    try {
        showElement('loadingAsientos');
        hideElement('emptyAsientos');
        hideElement('tablaAsientos');
        
        const params = new URLSearchParams({
            page: pagina,
            size: 20
        });
        
        const response = await httpGet(`/contabilidad/asientos/api?${params}`, {
            showLoading: false
        });
        
        hideElement('loadingAsientos');
        
        if (response && response.content && response.content.length > 0) {
            renderizarAsientos(response.content);
            actualizarPaginacion(response);
            showElement('tablaAsientos');
        } else {
            showElement('emptyAsientos');
        }
    } catch (error) {
        hideElement('loadingAsientos');
        showElement('emptyAsientos');
        console.error('Error al cargar asientos:', error);
    }
}

function renderizarAsientos(asientos) {
    const tbody = document.getElementById('bodyAsientos');
    tbody.innerHTML = '';
    const simbolo = escapeHtml(tbody.dataset.simbolo || '₡');
    
    asientos.forEach(asiento => {
        const tr = document.createElement('tr');
        
        // Badge de estado
        let estadoBadge = '';
        switch(asiento.estado) {
            case 'CONTABILIZADO':
                estadoBadge = '<span class="badge bg-success">Contabilizado</span>';
                break;
            case 'BORRADOR':
                estadoBadge = '<span class="badge bg-warning">Borrador</span>';
                break;
            case 'ANULADO':
                estadoBadge = '<span class="badge bg-danger">Anulado</span>';
                break;
        }
        
        // Badge de tipo
        let tipoBadge = '';
        switch(asiento.tipo) {
            case 'MANUAL':
                tipoBadge = '<span class="badge bg-asiento-manual">Manual</span>';
                break;
            case 'AUTOMATICO_FACTURA':
                tipoBadge = '<span class="badge bg-info">Auto-Factura</span>';
                break;
            case 'AUTOMATICO_PAGO':
                tipoBadge = '<span class="badge bg-success">Auto-Pago</span>';
                break;
            case 'AJUSTE':
                tipoBadge = '<span class="badge bg-warning">Ajuste</span>';
                break;
            case 'CIERRE':
                tipoBadge = '<span class="badge bg-secondary">Cierre</span>';
                break;
        }
        
        const id = Number(asiento.idAsiento);
        const concepto = escapeHtml(asiento.concepto);
        tr.innerHTML = `
            <td><strong>${escapeHtml(asiento.numero || '-')}</strong></td>
            <td>${escapeHtml(formatearFecha(asiento.fecha))}</td>
            <td>${tipoBadge || `<span class="badge bg-secondary">${escapeHtml(asiento.tipo)}</span>`}</td>
            <td class="text-truncate cell-truncate" title="${concepto}">${concepto}</td>
            <td class="text-end fw-semibold">${simbolo}${formatearNumero(asiento.totalDebe)}</td>
            <td class="text-end fw-semibold">${simbolo}${formatearNumero(asiento.totalHaber)}</td>
            <td class="text-center">${estadoBadge}</td>
            <td class="text-center">
                <div class="btn-group btn-group-sm" role="group">
                    <a href="/contabilidad/asientos/${id}"
                       class="btn btn-outline-secondary" title="Ver detalle">
                        <i class="fas fa-eye"></i>
                    </a>
                    ${asiento.puedeModificarse ? `
                    <a href="/contabilidad/asientos/${id}/editar"
                       class="btn btn-module-contabilidad" title="Editar">
                        <i class="fas fa-edit"></i>
                    </a>` : ''}
                    ${asiento.puedeContabilizarse ? `
                    <button type="button" class="btn btn-outline-success" 
                            onclick="contabilizarAsientoDesdeListado(${id})"
                            title="Contabilizar">
                        <i class="fas fa-check"></i>
                    </button>` : ''}
                    ${asiento.puedeRevertirse ? `
                    <button type="button" class="btn btn-outline-danger" 
                            onclick="revertirAsiento(${id})"
                            title="Revertir">
                        <i class="fas fa-undo"></i>
                    </button>` : ''}
                </div>
            </td>
        `;
        
        tbody.appendChild(tr);
    });
}

function actualizarPaginacion(pageData) {
    paginaActual = Number(pageData.number) || 0;
    totalPaginas = Number(pageData.totalPages) || 0;
    
    const paginaInfo = document.getElementById('paginaInfo');
    const inicio = pageData.number * pageData.size + 1;
    const fin = Math.min((pageData.number + 1) * pageData.size, pageData.totalElements);
    paginaInfo.textContent = `Mostrando ${inicio} a ${fin} de ${pageData.totalElements} asientos`;
    
    const paginacion = document.getElementById('paginacion');
    paginacion.innerHTML = '';
    
    // Botón Anterior
    const liAnterior = document.createElement('li');
    liAnterior.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    liAnterior.innerHTML = `<a class="page-link" href="#" onclick="cargarAsientos(${paginaActual - 1}); return false;">Anterior</a>`;
    paginacion.appendChild(liAnterior);
    
    // Páginas
    for (let i = 0; i < totalPaginas; i++) {
        if (i === 0 || i === totalPaginas - 1 || Math.abs(i - paginaActual) <= 2) {
            const li = document.createElement('li');
            li.className = `page-item ${i === paginaActual ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" onclick="cargarAsientos(${i}); return false;">${i + 1}</a>`;
            paginacion.appendChild(li);
        } else if (Math.abs(i - paginaActual) === 3) {
            const li = document.createElement('li');
            li.className = 'page-item disabled';
            li.innerHTML = '<span class="page-link">...</span>';
            paginacion.appendChild(li);
        }
    }
    
    // Botón Siguiente
    const liSiguiente = document.createElement('li');
    liSiguiente.className = `page-item ${paginaActual >= totalPaginas - 1 ? 'disabled' : ''}`;
    liSiguiente.innerHTML = `<a class="page-link" href="#" onclick="cargarAsientos(${paginaActual + 1}); return false;">Siguiente</a>`;
    paginacion.appendChild(liSiguiente);
}

async function aplicarFiltros() {
    const buscar = document.getElementById('txtBuscar').value;
    const tipo = document.getElementById('selTipo').value;
    const estado = document.getElementById('selEstado').value;
    const fechaInicio = document.getElementById('dtFechaInicio').value;
    const fechaFin = document.getElementById('dtFechaFin').value;
    
    try {
        showElement('loadingAsientos');
        hideElement('emptyAsientos');
        hideElement('tablaAsientos');
        
        const params = new URLSearchParams({ page: 0, size: 20 });
        if (buscar) params.append('buscar', buscar);
        if (tipo) params.append('tipo', tipo);
        if (estado) params.append('estado', estado);
        if (fechaInicio) params.append('fechaInicio', fechaInicio);
        if (fechaFin) params.append('fechaFin', fechaFin);
        
        const response = await httpGet(`/contabilidad/asientos/api/filtrar?${params}`, {
            showLoading: false
        });
        
        hideElement('loadingAsientos');
        
        if (response && response.content && response.content.length > 0) {
            renderizarAsientos(response.content);
            actualizarPaginacion(response);
            showElement('tablaAsientos');
        } else {
            showElement('emptyAsientos');
        }
    } catch (error) {
        hideElement('loadingAsientos');
        showElement('emptyAsientos');
        showToast('error', 'Error al aplicar filtros');
    }
}

// ============================================
// PÁGINA: FORMULARIO DE ASIENTO
// ============================================

async function initFormAsiento() {
    await cargarCuentasDisponibles();
    
    const idAsiento = document.getElementById('idAsiento')?.value;
    if (idAsiento) {
        await cargarAsiento(idAsiento);
    } else {
        agregarDetalle();
        agregarDetalle();
    }
}

async function cargarCuentasDisponibles() {
    try {
        cuentasDisponibles = await httpGet('/contabilidad/cuentas/api/activas-movimientos');
    } catch (error) {
        console.error('Error al cargar cuentas:', error);
        cuentasDisponibles = [];
    }
}

async function cargarAsiento(id) {
    try {
        const asiento = await httpGet(`/contabilidad/asientos/api/${id}`);
        
        if (asiento.detalles && asiento.detalles.length > 0) {
            detallesAsiento = asiento.detalles.map(d => ({
                id: contadorDetalle++,
                cuentaId: d.cuentaId,
                cuentaCodigo: d.cuentaCodigo,
                cuentaNombre: d.cuentaNombre,
                descripcion: d.descripcion ?? '',
                debe: d.debe,
                haber: d.haber
            }));
            
            renderizarDetalles();
        }
    } catch (error) {
        showToast('error', 'Error al cargar el asiento');
    }
}

function agregarDetalle() {
    const detalle = {
        id: contadorDetalle++,
        cuentaId: null,
        cuentaCodigo: '',
        cuentaNombre: '',
        descripcion: '',
        debe: 0,
        haber: 0
    };
    
    detallesAsiento.push(detalle);
    renderizarDetalles();
}

function eliminarDetalle(id) {
    detallesAsiento = detallesAsiento.filter(d => d.id !== id);
    renderizarDetalles();
}

function renderizarDetalles() {
    const tbody = document.getElementById('bodyDetalles');
    tbody.innerHTML = '';
    
    detallesAsiento.forEach(detalle => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                <select class="form-select form-select-sm" id="selCuenta${detalle.id}" required onchange="seleccionarCuenta(${detalle.id})">
                    <option value="">Seleccione cuenta...</option>
                    ${cuentasDisponibles.map(c => `
                        <option value="${Number(c.idCuenta)}" ${c.idCuenta === detalle.cuentaId ? 'selected' : ''}>
                            ${escapeHtml(c.codigo)} - ${escapeHtml(c.nombre)}
                        </option>
                    `).join('')}
                </select>
            </td>
            <td>
                <input type="text" class="form-control form-control-sm" id="txtConceptoDet${detalle.id}" 
                       value="${escapeHtml(detalle.descripcion)}" maxlength="500" 
                       onchange="actualizarDetalleDescripcion(${detalle.id}, this.value)">
            </td>
            <td>
                <input type="number" class="form-control form-control-sm text-end" id="txtDebe${detalle.id}" 
                       value="${Number(detalle.debe) || 0}" min="0" step="0.01" 
                       oninput="actualizarDetalleDebe(${detalle.id}, this.value)">
            </td>
            <td>
                <input type="number" class="form-control form-control-sm text-end" id="txtHaber${detalle.id}" 
                       value="${Number(detalle.haber) || 0}" min="0" step="0.01" 
                       oninput="actualizarDetalleHaber(${detalle.id}, this.value)">
            </td>
            <td class="text-center">
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="eliminarDetalle(${detalle.id})" title="Eliminar">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
    
    calcularTotales();
}

function seleccionarCuenta(detalleId) {
    const select = document.getElementById(`selCuenta${detalleId}`);
    const cuentaId = parseInt(select.value);
    
    const detalle = detallesAsiento.find(d => d.id === detalleId);
    if (detalle && cuentaId) {
        const cuenta = cuentasDisponibles.find(c => c.idCuenta === cuentaId);
        if (cuenta) {
            detalle.cuentaId = cuenta.idCuenta;
            detalle.cuentaCodigo = cuenta.codigo;
            detalle.cuentaNombre = cuenta.nombre;
        }
    }
}

function actualizarDetalleDescripcion(detalleId, descripcion) {
    const detalle = detallesAsiento.find(d => d.id === detalleId);
    if (detalle) {
        detalle.descripcion = descripcion;
    }
}

function actualizarDetalleDebe(detalleId, valor) {
    const detalle = detallesAsiento.find(d => d.id === detalleId);
    if (detalle) {
        detalle.debe = parseFloat(valor) || 0;
        if (detalle.debe > 0) {
            detalle.haber = 0;
            document.getElementById(`txtHaber${detalleId}`).value = 0;
        }
    }
    calcularTotales();
}

function actualizarDetalleHaber(detalleId, valor) {
    const detalle = detallesAsiento.find(d => d.id === detalleId);
    if (detalle) {
        detalle.haber = parseFloat(valor) || 0;
        if (detalle.haber > 0) {
            detalle.debe = 0;
            document.getElementById(`txtDebe${detalleId}`).value = 0;
        }
    }
    calcularTotales();
}

function calcularTotales() {
    const totalDebe = detallesAsiento.reduce((sum, d) => sum + (d.debe || 0), 0);
    const totalHaber = detallesAsiento.reduce((sum, d) => sum + (d.haber || 0), 0);
    const diferencia = Math.abs(totalDebe - totalHaber);
    
    // Obtener símbolo de moneda del elemento (ya renderizado por Thymeleaf)
    const simboloMoneda = escapeHtml(document.querySelector('#totalDebe span')?.textContent || '₡');
    
    document.getElementById('totalDebe').innerHTML = `<span>${simboloMoneda}</span>${formatearNumero(totalDebe)}`;
    document.getElementById('totalHaber').innerHTML = `<span>${simboloMoneda}</span>${formatearNumero(totalHaber)}`;
    
    const rowDiferencia = document.getElementById('rowDiferencia');
    const alertaCuadre = document.getElementById('alertaCuadre');
    const alertaExito = document.getElementById('alertaExito');
    const btnContabilizar = document.getElementById('btnGuardarContabilizar');
    
    if (diferencia > 0.01) {
        document.getElementById('diferencia').innerHTML = `<span>${simboloMoneda}</span>${formatearNumero(diferencia)}`;
        showElement('rowDiferencia');
        showElement('alertaCuadre');
        hideElement('alertaExito');
        if (btnContabilizar) btnContabilizar.disabled = true;
    } else {
        hideElement('rowDiferencia');
        hideElement('alertaCuadre');
        showElement('alertaExito');
        if (btnContabilizar) btnContabilizar.disabled = false;
    }
}

async function guardarAsiento(contabilizar = false) {
    const form = document.getElementById('formAsiento');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    if (detallesAsiento.length < 2) {
        showToast('error', 'Debe agregar al menos 2 movimientos');
        return;
    }
    
    const detallesValidos = detallesAsiento.filter(d => d.cuentaId && (d.debe > 0 || d.haber > 0));
    if (detallesValidos.length < 2) {
        showToast('error', 'Debe tener al menos 2 movimientos válidos');
        return;
    }
    
    const data = {
        fecha: document.getElementById('dtFecha').value,
        tipo: document.getElementById('selTipo').value,
        concepto: document.getElementById('txtConcepto').value,
        detalles: detallesValidos.map(d => ({
            cuentaId: d.cuentaId,
            descripcion: d.descripcion,
            debe: d.debe,
            haber: d.haber
        }))
    };
    
    const idAsiento = document.getElementById('idAsiento')?.value;
    
    try {
        let asiento;
        if (idAsiento) {
            asiento = await httpPut(`/contabilidad/asientos/api/${idAsiento}`, data);
        } else {
            asiento = await httpPost('/contabilidad/asientos/api', data);
        }
        
        if (contabilizar && asiento.idAsiento) {
            await httpPost(`/contabilidad/asientos/api/${asiento.idAsiento}/contabilizar`);
            showToast('success', 'Asiento guardado y contabilizado exitosamente');
        } else {
            showToast('success', 'Asiento guardado exitosamente');
        }
        
        setTimeout(() => window.location.href = '/contabilidad/asientos', 1500);
    } catch (error) {
        // El error ya se muestra en common.js
    }
}

// ============================================
// PÁGINA: DETALLE DE ASIENTO
// ============================================

function initDetalleAsiento() {
    // Inicialización si es necesaria
}

async function contabilizarAsiento() {
    if (typeof asientoId === 'undefined') return;
    
    const confirmar = await showConfirmDialog(
        '¿Está seguro de contabilizar este asiento?',
        'Una vez contabilizado, no podrá modificarlo.',
        'Sí, contabilizar'
    );
    
    if (confirmar) {
        try {
            await httpPost(`/contabilidad/asientos/api/${asientoId}/contabilizar`);
            showToast('success', 'Asiento contabilizado exitosamente');
            setTimeout(() => location.reload(), 1500);
        } catch (error) {
            // El error ya se muestra en common.js
        }
    }
}

/**
 * Revierte un asiento contabilizado (ADR-023): pide un motivo obligatorio, crea la reversión y
 * navega a ella. El original queda contabilizado y enlazado con su reversión.
 * @param {number} id - ID del asiento a revertir
 */
async function revertirAsiento(id) {
    const { value: motivo, isConfirmed } = await Swal.fire({
        title: '¿Revertir este asiento?',
        text: 'Se creará un asiento de reversión con las líneas invertidas, fechado hoy. El asiento original no se modifica.',
        icon: 'warning',
        input: 'textarea',
        inputLabel: 'Motivo de la reversión',
        inputPlaceholder: 'Indique el motivo (obligatorio)',
        inputAttributes: { maxlength: '400' },
        showCancelButton: true,
        confirmButtonColor: '#008040',
        cancelButtonColor: '#F44336',
        confirmButtonText: 'Sí, revertir',
        cancelButtonText: 'Cancelar',
        inputValidator: (valor) => (!valor || !valor.trim()) ? 'El motivo de la reversión es obligatorio' : undefined
    });

    if (!isConfirmed) {
        return;
    }

    try {
        const reversion = await httpPost(`/contabilidad/asientos/api/${Number(id)}/revertir`, { motivo: motivo.trim() });
        showToast('success', `Asiento revertido: se creó ${reversion.numero}`);
        setTimeout(() => window.location.href = `/contabilidad/asientos/${Number(reversion.idAsiento)}`, 1500);
    } catch (error) {
        // El error ya se muestra en common.js
    }
}

async function contabilizarAsientoDesdeListado(id) {
    const confirmar = await showConfirmDialog(
        '¿Está seguro de contabilizar este asiento?',
        'Una vez contabilizado, no podrá modificarlo.',
        'Sí, contabilizar'
    );
    
    if (confirmar) {
        try {
            await httpPost(`/contabilidad/asientos/api/${id}/contabilizar`);
            showToast('success', 'Asiento contabilizado exitosamente');
            await cargarAsientos(paginaActual);
        } catch (error) {
            // El error ya se muestra en common.js
        }
    }
}

// ============================================
// UTILIDADES
// ============================================

function formatearFecha(fecha) {
    if (!fecha) return '-';
    const d = new Date(fecha + 'T00:00:00');
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatearNumero(numero) {
    if (numero === null || numero === undefined) return '0.00';
    return parseFloat(numero).toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

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
    if (element) element.style.display = '';
}

function hideElement(id) {
    const element = document.getElementById(id);
    if (element) element.style.display = 'none';
}
