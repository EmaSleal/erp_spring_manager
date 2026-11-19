// T-042 (ADR-047 §1): listado de notas de crédito (Facturación → Notas de crédito). Datos de
// GET /api/facturas/notas-credito; acciones de comprobante iguales a las de comprobantes.html.
(function () {
    'use strict';

    const TAMANO = 20;
    let pagina = 0;
    let totalPaginas = 0;
    // T-043: ambiente que trae el enlace del panel; lo filtra el servidor
    let ambienteUrl = '';

    function $(id) {
        return document.getElementById(id);
    }

    function filtros() {
        return {
            desde: $('nc-filtro-desde').value,
            hasta: $('nc-filtro-hasta').value,
            cliente: $('nc-filtro-cliente').value,
            estado: $('nc-filtro-estado').value,
            ambiente: ambienteUrl
        };
    }

    function csrfHeaders() {
        const headers = {};
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (token && header) headers[header] = token;
        return headers;
    }

    function mensaje(texto) {
        const m = $('nc-mensaje');
        m.textContent = texto || '';
        m.style.display = texto ? '' : 'none';
    }

    function pintar(pag) {
        const cuerpo = $('nc-cuerpo');
        cuerpo.textContent = '';
        const acciones = {
            verFactura: (n) => openModal({ getAttribute: () => String(n.idFactura) }),
            consultar: consultarEstado,
            reenviar: reenviar
        };
        (pag.contenido || []).forEach((n) => cuerpo.appendChild(NotaCredito.filaNota(document, n, acciones)));
        pagina = pag.pagina;
        totalPaginas = pag.totalPaginas || 0;
        mensaje((pag.contenido || []).length ? '' : 'No hay notas de crédito para los filtros indicados');
        $('nc-pagina-info').textContent = NotaCredito.textoPagina(pag);
        $('nc-anterior').disabled = pagina <= 0;
        $('nc-siguiente').disabled = pagina + 1 >= totalPaginas;
    }

    async function cargar(nuevaPagina) {
        try {
            const res = await fetch('/api/facturas/notas-credito?' + NotaCredito.queryListado(filtros(), nuevaPagina, TAMANO));
            const cuerpo = await res.json().catch(() => null);
            if (!res.ok || !cuerpo || !cuerpo.data) {
                $('nc-cuerpo').textContent = '';
                mensaje(res.status === 403 ? 'No tiene permiso para consultar las notas de crédito'
                    : 'No se pudieron cargar las notas de crédito');
                return;
            }
            pintar(cuerpo.data);
        } catch (e) {
            mensaje('No se pudieron cargar las notas de crédito');
        }
    }

    async function reenviar(nota, boton) {
        if (!nota.idComprobante || !confirm('¿Reenviar comprobante a Hacienda?')) return;
        boton.disabled = true;
        try {
            const res = await fetch('/api/facturas/electronica/comprobantes/' + encodeURIComponent(nota.idComprobante) + '/reenviar',
                { method: 'POST', headers: csrfHeaders() });
            const cuerpo = await res.json().catch(() => ({}));
            const r = NotaCredito.resultadoReenvio(res.ok, cuerpo);
            showToast(r.tipo, r.mensaje);
        } catch (e) {
            showToast('error', 'Error reenviando comprobante');
        }
        cargar(pagina);
    }

    async function consultarEstado(nota, boton) {
        if (!nota.idComprobante) return;
        boton.disabled = true;
        try {
            const res = await fetch('/api/facturas/electronica/comprobantes/' + encodeURIComponent(nota.idComprobante)
                + '/consultar-estado-hacienda', { method: 'POST', headers: csrfHeaders() });
            const cuerpo = await res.json().catch(() => ({}));
            const r = NotaCredito.resultadoConsulta(res.status, cuerpo);
            showToast(r.tipo, r.mensaje);
            if (r.exito) {
                cargar(pagina);
                return;
            }
        } catch (e) {
            showToast('error', 'Error consultando el estado en Hacienda');
        }
        boton.disabled = false;
    }

    document.addEventListener('DOMContentLoaded', () => {
        $('nc-filtrar').addEventListener('click', () => cargar(0));
        $('nc-limpiar').addEventListener('click', () => {
            ['nc-filtro-desde', 'nc-filtro-hasta', 'nc-filtro-cliente', 'nc-filtro-estado'].forEach((id) => { $(id).value = ''; });
            ambienteUrl = '';
            pintarFiltroAmbiente();
            cargar(0);
        });
        $('nc-anterior').addEventListener('click', () => { if (pagina > 0) cargar(pagina - 1); });
        $('nc-siguiente').addEventListener('click', () => { if (pagina + 1 < totalPaginas) cargar(pagina + 1); });
        // T-043: el enlace del panel de Facturación trae ?estado=EN_TRAMITE
        const estadoUrl = (new URLSearchParams(window.location.search).get('estado') || '').trim();
        const selectEstado = $('nc-filtro-estado');
        if (Array.from(selectEstado.options).some((o) => o.value === estadoUrl)) {
            selectEstado.value = estadoUrl;
        }
        const ambiente = (new URLSearchParams(window.location.search).get('ambiente') || '').trim();
        ambienteUrl = /^[A-Z_]{1,30}$/.test(ambiente) ? ambiente : '';
        pintarFiltroAmbiente();
        cargar(0);
    });

    function pintarFiltroAmbiente() {
        const aviso = $('nc-filtro-ambiente');
        if (!aviso) return;
        aviso.textContent = ambienteUrl ? 'Solo notas del ambiente ' + ambienteUrl + ' (filtro del panel; «Limpiar» lo quita)' : '';
        aviso.style.display = ambienteUrl ? '' : 'none';
    }
})();
