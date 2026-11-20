// T-042 (ADR-047 §2): modal de emisión de la nota de crédito, abierto desde el modal de la factura (facturas.js).
// Todo el texto dinámico va por textContent; la lógica pura está en nota-credito.js (NotaCredito).
(function (global) {
    'use strict';

    let facturaActual = null;
    let enCurso = false;
    let navegando = false;

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

    function bloquear(bloqueado) {
        enCurso = bloqueado;
        $('nc-confirmar').disabled = bloqueado;
        $('nc-codigo').disabled = bloqueado;
        $('nc-razon').disabled = bloqueado;
    }

    /** Abre el modal con el resumen de la factura (FacturaDetalleDTO). */
    function abrir(detalle) {
        facturaActual = detalle;
        const r = NotaCredito.resumen(detalle);
        $('nc-resumen-factura').textContent = r.factura;
        $('nc-resumen-cliente').textContent = r.cliente;
        $('nc-resumen-monto').textContent = r.monto + ' ' + r.moneda;
        $('nc-aviso').textContent = r.aviso;
        $('nc-codigo').value = NotaCredito.CODIGOS[0].codigo;
        $('nc-razon').value = '';
        navegando = false;
        bloquear(false);
        mostrarError('');
        actualizarCodigo();
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
        mostrarError('');
        bloquear(true);
        try {
            const response = await fetch('/api/facturas/notas-credito', {
                method: 'POST',
                headers: csrfHeaders(),
                body: JSON.stringify(NotaCredito.cuerpoPedido(facturaActual.idFactura, $('nc-codigo').value,
                    $('nc-razon').value))
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
