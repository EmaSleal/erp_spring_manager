// ========================================
// Funciones de Facturación Electrónica
// Costa Rica v4.4
// ========================================

/**
 * Inicializar eventos al cargar la página
 */
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar visibilidad de campos FE
    const condicionVentaFE = document.getElementById('condicionVentaFE');
    const monedaFE = document.getElementById('monedaFE');
    
    if (condicionVentaFE) {
        togglePlazoCredito();
        condicionVentaFE.addEventListener('change', togglePlazoCredito);
    }
    
    if (monedaFE) {
        toggleTipoCambio();
        monedaFE.addEventListener('change', toggleTipoCambio);
    }
});

/**
 * Muestra/oculta el campo de plazo de crédito según la condición de venta
 */
function togglePlazoCredito() {
    const condicionVentaFE = document.getElementById('condicionVentaFE');
    const divPlazoCredito = document.getElementById('divPlazoCredito');
    const divPlazoCreditoHidden = document.getElementById('divPlazoCreditoHidden');
    const plazoCredito = document.getElementById('plazoCredito');
    
    if (!condicionVentaFE || !divPlazoCredito) return;
    
    const condicion = condicionVentaFE.value;
    
    if (condicion === 'CREDITO') {
        divPlazoCredito.style.display = 'block';
        if (divPlazoCreditoHidden) {
            divPlazoCreditoHidden.style.display = 'none';
        }
        if (plazoCredito) {
            plazoCredito.required = true;
            // Sugerir 30 días por defecto
            if (!plazoCredito.value) {
                plazoCredito.value = 30;
            }
        }
    } else {
        divPlazoCredito.style.display = 'none';
        if (divPlazoCreditoHidden) {
            divPlazoCreditoHidden.style.display = 'block';
        }
        if (plazoCredito) {
            plazoCredito.required = false;
            plazoCredito.value = '';
        }
    }
}

/**
 * Muestra/oculta el campo de tipo de cambio según la moneda
 */
function toggleTipoCambio() {
    const monedaFE = document.getElementById('monedaFE');
    const divTipoCambio = document.getElementById('divTipoCambio');
    const tipoCambio = document.getElementById('tipoCambio');
    
    if (!monedaFE || !divTipoCambio) return;
    
    const moneda = monedaFE.value;
    
    if (moneda === 'CRC') {
        // Colones costarricenses - no requiere tipo de cambio
        divTipoCambio.style.display = 'none';
        if (tipoCambio) {
            tipoCambio.required = false;
            tipoCambio.value = '1.00000';
        }
    } else {
        // USD o EUR - requiere tipo de cambio
        divTipoCambio.style.display = 'block';
        if (tipoCambio) {
            tipoCambio.required = true;
            // Limpiar valor para que el usuario ingrese el tipo de cambio actual
            if (!tipoCambio.value || tipoCambio.value === '1.00000') {
                tipoCambio.value = '';
                // Placeholder según moneda
                if (moneda === 'USD') {
                    tipoCambio.placeholder = 'Ej: 532.15000 (1 USD = 532.15 CRC)';
                } else if (moneda === 'EUR') {
                    tipoCambio.placeholder = 'Ej: 580.50000 (1 EUR = 580.50 CRC)';
                }
            }
        }
    }
}

/**
 * Validar campos de FE antes de guardar factura
 */
function validarCamposFE() {
    const condicionVentaFE = document.getElementById('condicionVentaFE');
    const medioPagoFE = document.getElementById('medioPagoFE');
    const monedaFE = document.getElementById('monedaFE');
    const tipoCambio = document.getElementById('tipoCambio');
    const plazoCredito = document.getElementById('plazoCredito');
    
    // Validar condición de venta
    if (!condicionVentaFE || !condicionVentaFE.value) {
        Swal.fire({
            icon: 'warning',
            title: 'Campo requerido',
            text: 'Debe seleccionar una condición de venta',
            confirmButtonText: 'Entendido'
        });
        condicionVentaFE?.focus();
        return false;
    }
    
    // Validar medio de pago
    if (!medioPagoFE || !medioPagoFE.value) {
        Swal.fire({
            icon: 'warning',
            title: 'Campo requerido',
            text: 'Debe seleccionar un medio de pago',
            confirmButtonText: 'Entendido'
        });
        medioPagoFE?.focus();
        return false;
    }
    
    // Validar moneda
    if (!monedaFE || !monedaFE.value) {
        Swal.fire({
            icon: 'warning',
            title: 'Campo requerido',
            text: 'Debe seleccionar una moneda',
            confirmButtonText: 'Entendido'
        });
        monedaFE?.focus();
        return false;
    }
    
    // Si es crédito, validar plazo
    if (condicionVentaFE.value === 'CREDITO') {
        if (!plazoCredito || !plazoCredito.value || plazoCredito.value <= 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Campo requerido',
                text: 'Debe ingresar el plazo de crédito en días (mayor a 0)',
                confirmButtonText: 'Entendido'
            });
            plazoCredito?.focus();
            return false;
        }
    }
    
    // Si la moneda no es CRC, validar tipo de cambio
    if (monedaFE.value !== 'CRC') {
        if (!tipoCambio || !tipoCambio.value || parseFloat(tipoCambio.value) <= 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Campo requerido',
                text: 'Debe ingresar el tipo de cambio (mayor a 0)',
                confirmButtonText: 'Entendido'
            });
            tipoCambio?.focus();
            return false;
        }
    }
    
    return true;
}

/**
 * Calcular fecha de pago basado en condición de venta
 */
function calcularFechaPago() {
    const fechaEntrega = document.getElementById('fechaEntrega');
    const fechaPago = document.getElementById('fechaPago');
    const condicionVentaFE = document.getElementById('condicionVentaFE');
    const plazoCredito = document.getElementById('plazoCredito');
    
    if (!fechaEntrega || !fechaPago || !fechaEntrega.value) return;
    
    const fecha = new Date(fechaEntrega.value + 'T00:00:00');
    
    // Si es crédito y hay plazo, sumar días
    if (condicionVentaFE && condicionVentaFE.value === 'CREDITO' && plazoCredito && plazoCredito.value) {
        fecha.setDate(fecha.getDate() + parseInt(plazoCredito.value));
    } else {
        // Por defecto +7 días
        fecha.setDate(fecha.getDate() + 7);
    }
    
    // Formatear fecha a YYYY-MM-DD
    const year = fecha.getFullYear();
    const month = String(fecha.getMonth() + 1).padStart(2, '0');
    const day = String(fecha.getDate()).padStart(2, '0');
    
    fechaPago.value = `${year}-${month}-${day}`;
}
