// T-042 (ADR-047 §2): modal de emisión de la nota de crédito, abierto desde el modal de la factura (facturas.js).
// Todo el texto dinámico va por textContent; la lógica pura está en nota-credito.js (NotaCredito).
(function (global) {
    'use strict';

    let facturaActual = null;
    let enCurso = false;
    let navegando = false;
    let modalidad = 'TOTAL';

    function $(id) {
        return document.getElementById(id);
    }

    function csrfHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (token && header) headers[header] = token;
        return headers;
    }

    function mostrarError(texto) {
        const caja = $('nc-error');
        caja.textContent = texto || '';
        caja.style.display = texto ? '' : 'none';
    }

    function actualizarRazon() {
        const razon = $('nc-razon').value;
        $('nc-contador').textContent = NotaCredito.contadorRazon(razon);
        const error = razon.length === 0 ? null : NotaCredito.validarRazon(razon);
        $('nc-razon-error').textContent = error || '';
        $('nc-razon').classList.toggle('is-invalid', !!error);
    }

    function actualizarCodigo() {
        const codigo = $('nc-codigo').value;
        const periodo = facturaActual ? facturaActual.periodoFacturaOriginal : null;
        $('nc-efecto').textContent = NotaCredito.textoEfecto(codigo, periodo);
        // No bloquea la emisión (ADR-050 §4)
        const aviso = $('nc-advertencia');
        const texto = NotaCredito.advertenciaMismoMes(codigo, periodo, new Date());
        aviso.textContent = texto;
        aviso.style.display = texto ? '' : 'none';
    }

    function lineasServidor() {
        return facturaActual && Array.isArray(facturaActual.notaCreditoLineas) ? facturaActual.notaCreditoLineas : [];
    }

    function moneda() {
        return (facturaActual && facturaActual.monedaFE) || 'CRC';
    }

    /** Montos escritos por línea: {idLineaFactura: texto}. */
    function entradas() {
        const mapa = {};
        document.querySelectorAll('#nc-lineas input[data-id-linea]').forEach((i) => {
            mapa[i.getAttribute('data-id-linea')] = i.value;
        });
        return mapa;
    }

    /** Total en vivo, errores por línea y por factura; habilita Confirmar solo si todo es válido. */
    function actualizarParcial() {
        if (modalidad !== 'PARCIAL') {
            $('nc-confirmar').disabled = enCurso;
            return null;
        }
        const r = NotaCredito.calcularParcial(lineasServidor(), entradas(), facturaActual.notaCreditoDisponible);
        $('nc-total-nota').textContent = NotaCredito.formatearMonto(r.total, moneda());
        document.querySelectorAll('#nc-lineas input[data-id-linea]').forEach((i) => {
            const error = r.errores[i.getAttribute('data-id-linea')] || '';
            i.classList.toggle('is-invalid', !!error);
            const caja = i.parentElement.querySelector('.nc-monto-error');
            if (caja) caja.textContent = error;
        });
        const hayTexto = Object.values(entradas()).some((t) => t.trim() !== '');
        $('nc-parcial-error').textContent = hayTexto ? r.errorGeneral : '';
        $('nc-confirmar').disabled = enCurso || !r.valido;
        return r;
    }

    function bloquear(bloqueado) {
        enCurso = bloqueado;
        $('nc-confirmar').disabled = bloqueado;
        $('nc-codigo').disabled = bloqueado;
        $('nc-razon').disabled = bloqueado;
        document.querySelectorAll('#nc-lineas input, input[name="nc-modalidad"]').forEach((i) => { i.disabled = bloqueado; });
        if (!bloqueado) actualizarParcial();
    }

    /** Llena el selector con los códigos de la modalidad (textContent) y pinta la tabla de líneas si es parcial. */
    function aplicarModalidad(nueva) {
        modalidad = nueva;
        const select = $('nc-codigo');
        select.textContent = '';
        NotaCredito.codigosDe(modalidad).forEach((c) => {
            const o = document.createElement('option');
            o.value = c.codigo;
            o.textContent = c.codigo + ' - ' + c.nombre;
            select.appendChild(o);
        });
        const parcial = modalidad === 'PARCIAL';
        $('nc-parcial').style.display = parcial ? '' : 'none';
        $('nc-aviso').style.display = parcial ? 'none' : '';
        if (parcial) {
            const cuerpo = $('nc-lineas');
            cuerpo.textContent = '';
            NotaCredito.pintarLineasParcial(document, cuerpo, lineasServidor(), moneda(), actualizarParcial);
            $('nc-disponible-factura').textContent =
                NotaCredito.formatearMonto(facturaActual.notaCreditoDisponible, moneda());
        }
        actualizarCodigo();
        actualizarParcial();
    }

    /** Abre el modal con el resumen de la factura (FacturaDetalleDTO). */
    function abrir(detalle) {
        facturaActual = detalle;
        const r = NotaCredito.resumen(detalle);
        $('nc-resumen-factura').textContent = r.factura;
        $('nc-resumen-cliente').textContent = r.cliente;
        $('nc-resumen-monto').textContent = r.monto + ' ' + r.moneda;
        $('nc-aviso').textContent = r.aviso;
        $('nc-razon').value = '';
        // Las banderas y el disponible vienen del servidor: total solo si puedeEmitirNotaTotal, parcial si puedeEmitirNotaParcial
        const totalOk = facturaActual.puedeEmitirNotaTotal !== false;
        const parcialOk = facturaActual.puedeEmitirNotaParcial === true;
        $('nc-modalidad-total').disabled = !totalOk;
        $('nc-modalidad-parcial').disabled = !parcialOk;
        const motivo = !totalOk && facturaActual.motivoNotaTotalNoDisponible ? facturaActual.motivoNotaTotalNoDisponible : '';
        $('nc-modalidad-motivo').textContent = motivo;
        const inicial = totalOk ? 'TOTAL' : 'PARCIAL';
        $('nc-modalidad-total').checked = inicial === 'TOTAL';
        $('nc-modalidad-parcial').checked = inicial === 'PARCIAL';
        navegando = false;
        bloquear(false);
        mostrarError('');
        aplicarModalidad(inicial);
        actualizarRazon();
        // El modal de la factura se cierra antes para no apilar modales; se reabre si se cancela.
        const emision = bootstrap.Modal.getOrCreateInstance($('notaCreditoModal'));
        const base = $('facturaModal');
        const baseInstancia = base ? bootstrap.Modal.getInstance(base) : null;
        if (base && base.classList.contains('show') && baseInstancia) {
            base.addEventListener('hidden.bs.modal', () => emision.show(), { once: true });
            baseInstancia.hide();
        } else {
            emision.show();
        }
    }

    async function confirmar() {
        if (enCurso || !facturaActual) return;
        const error = NotaCredito.validarRazon($('nc-razon').value);
        if (error) {
            $('nc-razon-error').textContent = error;
            $('nc-razon').classList.add('is-invalid');
            return;
        }
        let lineas = null;
        if (modalidad === 'PARCIAL') {
            const r = actualizarParcial();
            if (!r || !r.valido) return;
            lineas = r.lineas;
        }
        mostrarError('');
        bloquear(true);
        try {
            const response = await fetch('/api/facturas/notas-credito', {
                method: 'POST',
                headers: csrfHeaders(),
                body: JSON.stringify(NotaCredito.cuerpoPedido(facturaActual.idFactura, $('nc-codigo').value,
                    $('nc-razon').value, lineas))
            });
            const cuerpo = await response.json().catch(() => null);
            if (!response.ok || !cuerpo || !cuerpo.data || cuerpo.data.id == null) {
                mostrarError(NotaCredito.mensajeDeError(response.ok ? 0 : response.status, cuerpo));
                bloquear(false);
                return;
            }
            navegando = true;
            if (typeof showToast === 'function') showToast('success', NotaCredito.mensajeResultado(cuerpo));
            // Breve pausa para que el usuario alcance a ver el mensaje antes del cambio de página
            setTimeout(() => { window.location.href = '/facturas/notas-credito/' + encodeURIComponent(cuerpo.data.id); }, 1200);
        } catch (e) {
            mostrarError(NotaCredito.mensajeDeError(0, null));
            bloquear(false);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const modal = $('notaCreditoModal');
        if (!modal) return;
        $('nc-codigo').addEventListener('change', actualizarCodigo);
        document.querySelectorAll('input[name="nc-modalidad"]').forEach((r) => {
            r.addEventListener('change', () => { if (r.checked) aplicarModalidad(r.value); });
        });
        $('nc-razon').addEventListener('input', actualizarRazon);
        $('nc-confirmar').addEventListener('click', confirmar);
        modal.addEventListener('hidden.bs.modal', () => {
            // Cancelar vuelve al detalle de la factura
            if (!navegando && facturaActual && typeof openModal === 'function') {
                openModal({ getAttribute: () => String(facturaActual.idFactura) });
            }
        });
    });

    global.NotaCreditoModal = { abrir };
})(typeof window !== 'undefined' ? window : globalThis);
