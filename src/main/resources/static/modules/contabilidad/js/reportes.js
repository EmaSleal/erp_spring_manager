// ============================================
// REPORTES CONTABLES - JAVASCRIPT
// ============================================

// Variables globales
let modalLibroMayor, modalBalanceGeneral;
let cuentasDisponibles = [];

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    const currentPath = window.location.pathname;
    
    if (currentPath === '/contabilidad/reportes' || currentPath === '/contabilidad/reportes/') {
        initIndexReportes();
    } else if (currentPath.includes('/libro-mayor')) {
        initLibroMayor();
    }
    
    // Establecer fecha actual por defecto
    const hoy = new Date().toISOString().split('T')[0];
    const dtFechaBalanceGeneral = document.getElementById('dtFechaBalanceGeneral');
    if (dtFechaBalanceGeneral) {
        dtFechaBalanceGeneral.value = hoy;
    }
});

// ============================================
// PÁGINA: INDEX DE REPORTES
// ============================================

async function initIndexReportes() {
    // Inicializar modales
    const modalLibroMayorElement = document.getElementById('modalLibroMayor');
    const modalBalanceGeneralElement = document.getElementById('modalBalanceGeneral');
    
    if (modalLibroMayorElement) {
        modalLibroMayor = new bootstrap.Modal(modalLibroMayorElement);
    }
    
    if (modalBalanceGeneralElement) {
        modalBalanceGeneral = new bootstrap.Modal(modalBalanceGeneralElement);
    }
    
    // Cargar cuentas disponibles
    await cargarCuentasParaReportes();
}

async function cargarCuentasParaReportes() {
    try {
        const cuentas = await httpGet('/contabilidad/cuentas/api/todas');
        cuentasDisponibles = cuentas;
        
        // Poblar select de libro mayor
        const selCuentaLibroMayor = document.getElementById('selCuentaLibroMayor');
        if (selCuentaLibroMayor) {
            selCuentaLibroMayor.innerHTML = '<option value="">Seleccione una cuenta...</option>';
            cuentas.forEach(cuenta => {
                const option = document.createElement('option');
                option.value = cuenta.idCuenta;
                option.textContent = `${cuenta.codigo} - ${cuenta.nombre}`;
                selCuentaLibroMayor.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error al cargar cuentas:', error);
    }
}

function establecerPeriodo(tipo) {
    const hoy = new Date();
    let fechaInicio, fechaFin = hoy;
    
    switch(tipo) {
        case 'mes':
            fechaInicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
            break;
        case 'trimestre':
            const mesActual = hoy.getMonth();
            const mesInicioTrimestre = Math.floor(mesActual / 3) * 3;
            fechaInicio = new Date(hoy.getFullYear(), mesInicioTrimestre, 1);
            break;
        case 'ano':
            fechaInicio = new Date(hoy.getFullYear(), 0, 1);
            break;
    }
    
    const dtFechaInicio = document.getElementById('dtFechaInicio');
    const dtFechaFin = document.getElementById('dtFechaFin');
    
    if (dtFechaInicio) dtFechaInicio.value = fechaInicio.toISOString().split('T')[0];
    if (dtFechaFin) dtFechaFin.value = fechaFin.toISOString().split('T')[0];
}

function abrirLibroMayor() {
    if (modalLibroMayor) {
        modalLibroMayor.show();
    }
}

function generarLibroMayor() {
    const cuentaId = document.getElementById('selCuentaLibroMayor').value;
    const fechaInicio = document.getElementById('dtFechaInicio').value;
    const fechaFin = document.getElementById('dtFechaFin').value;
    
    if (!cuentaId) {
        showToast('error', 'Debe seleccionar una cuenta');
        return;
    }
    
    if (!fechaInicio || !fechaFin) {
        showToast('error', 'Debe especificar el período');
        return;
    }
    
    const params = new URLSearchParams({
        cuentaId,
        fechaInicio,
        fechaFin
    });
    
    window.location.href = `/contabilidad/reportes/libro-mayor?${params}`;
}

function abrirBalanceGeneral() {
    if (modalBalanceGeneral) {
        modalBalanceGeneral.show();
    }
}

function generarBalanceGeneral() {
    const fecha = document.getElementById('dtFechaBalanceGeneral').value;
    
    if (!fecha) {
        showToast('error', 'Debe seleccionar una fecha');
        return;
    }
    
    window.location.href = `/contabilidad/reportes/balance-general?fecha=${fecha}`;
}

// ============================================
// PÁGINA: LIBRO MAYOR
// ============================================

async function initLibroMayor() {
    await cargarCuentasParaLibroMayor();
}

async function cargarCuentasParaLibroMayor() {
    try {
        const cuentas = await httpGet('/contabilidad/cuentas/api/todas');
        
        const selCuenta = document.getElementById('selCuenta');
        if (selCuenta) {
            selCuenta.innerHTML = '<option value="">Seleccione una cuenta...</option>';
            cuentas.forEach(cuenta => {
                const option = document.createElement('option');
                option.value = cuenta.idCuenta;
                option.textContent = `${cuenta.codigo} - ${cuenta.nombre}`;
                selCuenta.appendChild(option);
            });
            
            // Seleccionar cuenta actual si existe en URL
            const urlParams = new URLSearchParams(window.location.search);
            const cuentaId = urlParams.get('cuentaId');
            if (cuentaId) {
                selCuenta.value = cuentaId;
            }
        }
    } catch (error) {
        console.error('Error al cargar cuentas:', error);
    }
}

function cambiarCuenta() {
    generarReporte();
}

function generarReporte() {
    const cuentaId = document.getElementById('selCuenta').value;
    const fechaInicio = document.getElementById('dtFechaInicio').value;
    const fechaFin = document.getElementById('dtFechaFin').value;
    
    if (!cuentaId) {
        showToast('error', 'Debe seleccionar una cuenta');
        return;
    }
    
    if (!fechaInicio || !fechaFin) {
        showToast('error', 'Debe especificar el período');
        return;
    }
    
    const params = new URLSearchParams({
        cuentaId,
        fechaInicio,
        fechaFin
    });
    
    window.location.href = `/contabilidad/reportes/libro-mayor?${params}`;
}

// ============================================
// EXPORTACIÓN
// ============================================

function exportarPDF() {
    showToast('info', 'Funcionalidad de exportación en desarrollo');
    
    // TODO: Implementar exportación a PDF
    // Opciones:
    // 1. Usar jsPDF en frontend
    // 2. Endpoint backend con iText/PDFBox
    // 3. Imprimir página (window.print())
}

function exportarExcel() {
    showToast('info', 'Funcionalidad de exportación en desarrollo');
    
    // TODO: Implementar exportación a Excel
    // Opciones:
    // 1. Usar SheetJS (xlsx) en frontend
    // 2. Endpoint backend con Apache POI
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
