// ============================================================================
// CLIENTE-ERRORES.JS - Pinta junto a cada campo los errores 400 de /clientes/guardar (T-038, ADR-044)
// Cada campo validado tiene un contenedor <div class="invalid-feedback" data-error-for="<campo>">.
// El texto del servidor se asigna con textContent (nunca innerHTML).
// ============================================================================

/** Quita los errores pintados: texto, is-invalid del campo y contenedor oculto. */
function limpiarErroresCliente(form) {
    if (!form) return;
    form.querySelectorAll('[data-error-for]').forEach(function (contenedor) {
        contenedor.textContent = '';
        contenedor.style.display = 'none';
    });
    form.querySelectorAll('.is-invalid').forEach(function (campo) {
        campo.classList.remove('is-invalid');
    });
}

/**
 * Pinta los errores [{campo, mensaje}] junto a cada campo.
 * Devuelve los errores que no tienen contenedor en el formulario, para que quien llama los muestre de otra forma.
 */
function pintarErroresCliente(form, errores) {
    const sinContenedor = [];
    if (!form) return sinContenedor;
    limpiarErroresCliente(form);
    (Array.isArray(errores) ? errores : []).forEach(function (error) {
        const campo = error && typeof error.campo === 'string' ? error.campo : '';
        const mensaje = error && error.mensaje != null ? String(error.mensaje) : '';
        // El nombre del campo viene del servidor: solo letras, para usarlo con seguridad en un selector.
        const contenedor = /^[A-Za-z]+$/.test(campo) ? form.querySelector('[data-error-for="' + campo + '"]') : null;
        if (!contenedor) {
            sinContenedor.push(error);
            return;
        }
        contenedor.textContent = contenedor.textContent ? contenedor.textContent + ' ' + mensaje : mensaje;
        contenedor.style.display = 'block';
        const input = form.querySelector('[name="' + campo + '"]');
        if (input) input.classList.add('is-invalid');
    });
    return sinContenedor;
}
