/**
 * Selector de tipo de cambio de un documento (ADR-011), compartido por ordenes-compra.html
 * y cuentas-pagar.html.
 *
 * En CRC oculta la fila del tipo de cambio y limpia el campo. En moneda extranjera muestra la
 * fila y precarga la tasa de venta registrada para la fecha indicada (editable); si no hay
 * tasa, deja el campo vacío para que el usuario la digite.
 *
 * @param {Object} opciones
 * @param {string} opciones.moneda  código de la moneda seleccionada (CRC, USD, EUR)
 * @param {HTMLElement} opciones.fila  contenedor del tipo de cambio (se oculta en CRC)
 * @param {HTMLInputElement} opciones.input  campo del tipo de cambio
 * @param {HTMLElement} opciones.ayuda  texto de ayuda bajo el campo
 * @param {string} [opciones.fecha]  fecha ISO (yyyy-MM-dd) de la tasa; por defecto hoy
 */
function actualizarTipoCambioDocumento({ moneda, fila, input, ayuda, fecha }) {
    if (!moneda || moneda === 'CRC') {
        fila.style.display = 'none';
        input.value = '';
        return;
    }
    fila.style.display = '';
    const dia = fecha || new Date().toISOString().substring(0, 10);
    fetch(`/api/tipos-cambio/venta?moneda=${moneda}&fecha=${dia}`)
        .then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) }))
        .then(({ ok, data }) => {
            if (ok && data && data.tasaVenta) {
                input.value = data.tasaVenta;
                ayuda.textContent = `Tasa de venta de ${moneda} al ${dia}; puede editarla`;
            } else {
                input.value = '';
                ayuda.textContent = (data && data.message) || `No hay tipo de cambio de ${moneda}; ingréselo`;
            }
        })
        .catch(() => { /* el usuario puede digitarlo */ });
}
