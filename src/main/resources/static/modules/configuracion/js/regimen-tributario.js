/**
 * T-056 (ADR-072): pantalla de régimen tributario con vigencia (Configuración). Lista el historial de tramos, marca el
 * vigente hoy y el próximo, y permite agregar, editar o borrar tramos futuros. Todo se pinta con createElement y
 * textContent: ningún dato del servidor se interpreta como HTML. Las funciones puras se exponen en
 * window.RegimenTributarioUI para probarlas con node --test sin DOM real.
 */
(function (global) {
    'use strict';

    const REGIMENES = { GENERAL: 'Régimen general', SIMPLIFICADO: 'Régimen simplificado' };
    const LARGO_NOTA = 300;
    const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;

    function etiquetaRegimen(regimen) {
        return Object.prototype.hasOwnProperty.call(REGIMENES, regimen) ? REGIMENES[regimen] : String(regimen == null ? '' : regimen);
    }

    function fechaIsoValida(valor) {
        const m = FECHA_ISO.exec(String(valor == null ? '' : valor));
        if (!m) return false;
        const d = new Date(Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])));
        return d.getUTCFullYear() === Number(m[1]) && d.getUTCMonth() === Number(m[2]) - 1 && d.getUTCDate() === Number(m[3]);
    }

    /**
     * Valida el formulario antes de enviarlo. {@code hoy} es la fecha ISO de hoy en Costa Rica que da el servidor.
     * @returns {string|null} el mensaje del primer error, o null si es válido
     */
    function validarTramo(tramo, hoy) {
        const t = tramo || {};
        if (!Object.prototype.hasOwnProperty.call(REGIMENES, t.regimen)) {
            return 'Indique el régimen (general o simplificado)';
        }
        if (!fechaIsoValida(t.vigenteDesde)) {
            return 'Indique una fecha de inicio válida';
        }
        if (fechaIsoValida(hoy) && t.vigenteDesde <= hoy) {
            return 'La fecha de inicio debe ser posterior a hoy';
        }
        if (t.nota != null && String(t.nota).length > LARGO_NOTA) {
            return 'La nota no puede exceder ' + LARGO_NOTA + ' caracteres';
        }
        return null;
    }

    /** {@code hoy}: fecha ISO de hoy en Costa Rica que da el servidor; rotula los tramos que no son vigente ni próximo. */
    function estadoTramo(tramo, hoy) {
        if (tramo.vigente) return 'Vigente hoy';
        if (tramo.proximo) return 'Próximo';
        return tramo.vigenteDesde && hoy && tramo.vigenteDesde > hoy ? 'Futuro' : 'Anterior';
    }

    function celda(doc, texto, clase) {
        const td = doc.createElement('td');
        if (clase) td.className = clase;
        td.textContent = texto === null || texto === undefined || texto === '' ? '—' : String(texto);
        return td;
    }

    function boton(doc, icono, titulo, clase, onClick) {
        const btn = doc.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-sm ' + clase + ' me-1';
        btn.title = titulo;
        btn.setAttribute('aria-label', titulo);
        const i = doc.createElement('i');
        i.className = icono;
        btn.appendChild(i);
        btn.addEventListener('click', onClick);
        return btn;
    }

    /**
     * @param {Element} cuerpo tbody a llenar
     * @param {Array} lista tramos tal como los da la API (con vigente, proximo, editable, bloqueo)
     * @param {{puedeGestionar: boolean, hoy: string, onEditar: Function, onEliminar: Function}} opciones
     */
    function pintarHistorial(cuerpo, lista, opciones, doc) {
        const d = doc || global.document;
        const op = opciones || {};
        cuerpo.replaceChildren();
        if (!lista || !lista.length) {
            const tr = d.createElement('tr');
            const td = celda(d, 'No hay tramos de régimen registrados', 'text-center text-muted py-3');
            td.setAttribute('colspan', '6');
            tr.appendChild(td);
            cuerpo.appendChild(tr);
            return;
        }
        lista.forEach((t) => {
            const tr = d.createElement('tr');
            if (t.vigente) tr.className = 'table-success';
            else if (t.proximo) tr.className = 'table-warning';
            tr.appendChild(celda(d, t.vigenteDesde, 'fw-bold'));
            tr.appendChild(celda(d, etiquetaRegimen(t.regimen)));
            tr.appendChild(celda(d, estadoTramo(t, op.hoy)));
            tr.appendChild(celda(d, t.nota));
            tr.appendChild(celda(d, t.usuarioNombre));
            const acciones = d.createElement('td');
            acciones.className = 'text-center';
            if (op.puedeGestionar && t.editable) {
                acciones.appendChild(boton(d, 'fas fa-edit', 'Editar el tramo desde ' + t.vigenteDesde, 'btn-outline-primary',
                    () => op.onEditar && op.onEditar(t)));
                acciones.appendChild(boton(d, 'fas fa-trash', 'Borrar el tramo desde ' + t.vigenteDesde, 'btn-outline-danger',
                    () => op.onEliminar && op.onEliminar(t)));
            } else if (t.bloqueo) {
                acciones.title = t.bloqueo;
                acciones.textContent = '🔒';
            }
            tr.appendChild(acciones);
            cuerpo.appendChild(tr);
        });
    }

    global.RegimenTributarioUI = { validarTramo, etiquetaRegimen, estadoTramo, pintarHistorial, fechaIsoValida, LARGO_NOTA };

    // ── Página (solo con DOM real) ──────────────────────────────────────────────

    const doc = global.document;
    if (!doc || typeof doc.addEventListener !== 'function') return;

    let tramoEnEdicion = null;
    let puedeGestionar = false;
    let hoyActual = null;

    function porId(id) { return doc.getElementById(id); }

    function cabeceras() {
        const h = { 'Content-Type': 'application/json' };
        if (global.APP_CONFIG && global.APP_CONFIG.csrf) h[global.APP_CONFIG.csrf.header] = global.APP_CONFIG.csrf.token;
        return h;
    }

    function toast(mensaje, tipo) {
        if (typeof global.mostrarToast === 'function') { global.mostrarToast(mensaje, tipo); return; }
        const c = porId('toastContainer');
        if (!c || !global.bootstrap) return;
        const t = doc.createElement('div');
        t.className = 'toast align-items-center text-bg-' + (tipo || 'primary') + ' border-0';
        t.setAttribute('role', 'alert');
        const cuerpo = doc.createElement('div');
        cuerpo.className = 'toast-body';
        cuerpo.textContent = mensaje;
        t.appendChild(cuerpo);
        c.appendChild(t);
        new global.bootstrap.Toast(t, { delay: 4000 }).show();
    }

    function vaciarFormulario() {
        tramoEnEdicion = null;
        porId('formRegimen').reset();
        porId('tituloFormRegimen').textContent = 'Agregar tramo';
        porId('regimenError').textContent = '';
    }

    function editar(t) {
        tramoEnEdicion = t.id;
        porId('regimenSel').value = t.regimen;
        porId('regimenDesde').value = t.vigenteDesde;
        porId('regimenNota').value = t.nota || '';
        porId('tituloFormRegimen').textContent = 'Editar el tramo desde ' + t.vigenteDesde;
        porId('regimenError').textContent = '';
    }

    async function eliminar(t) {
        if (!global.confirm('¿Borrar el tramo desde ' + t.vigenteDesde + '?')) return;
        try {
            const resp = await fetch('/api/regimen-tributario/' + encodeURIComponent(t.id), { method: 'DELETE', headers: cabeceras() });
            const data = await resp.json().catch(() => ({}));
            if (resp.ok && data.success) { toast(data.message || 'Tramo borrado.', 'success'); vaciarFormulario(); cargar(); }
            else toast(data.message || 'No se pudo borrar el tramo.', 'danger');
        } catch (e) {
            toast('Error de conexión. Intente nuevamente.', 'danger');
        }
    }

    async function cargar() {
        try {
            const resp = await fetch('/api/regimen-tributario');
            const json = resp.ok ? await resp.json() : null;
            const datos = (json && json.data) || [];
            pintarHistorial(porId('regimenCuerpo'), datos, { puedeGestionar, hoy: hoyActual, onEditar: editar, onEliminar: eliminar }, doc);
        } catch (e) {
            toast('Error al cargar el historial de régimen.', 'danger');
        }
    }

    async function guardar(evento) {
        evento.preventDefault();
        const tramo = {
            regimen: porId('regimenSel').value,
            vigenteDesde: porId('regimenDesde').value,
            nota: porId('regimenNota').value
        };
        const error = porId('regimenError');
        error.textContent = '';
        const invalido = validarTramo(tramo, hoyActual);
        if (invalido) { error.textContent = invalido; return; }
        const btn = porId('btnGuardarRegimen');
        btn.disabled = true;
        try {
            const editando = tramoEnEdicion !== null;
            const resp = await fetch(editando ? '/api/regimen-tributario/' + encodeURIComponent(tramoEnEdicion) : '/api/regimen-tributario', {
                method: editando ? 'PUT' : 'POST',
                headers: cabeceras(),
                body: JSON.stringify(tramo)
            });
            const data = await resp.json().catch(() => ({}));
            if (resp.ok && data.success) { toast(data.message || 'Guardado.', 'success'); vaciarFormulario(); cargar(); }
            else error.textContent = data.message || 'No se pudo guardar el tramo.';
        } catch (e) {
            error.textContent = 'Error de conexión. Intente nuevamente.';
        } finally {
            btn.disabled = false;
        }
    }

    doc.addEventListener('DOMContentLoaded', () => {
        const tabla = porId('tablaRegimen');
        if (!tabla) return;
        puedeGestionar = tabla.getAttribute('data-puede-gestionar') === 'true';
        hoyActual = tabla.getAttribute('data-hoy');
        const form = porId('formRegimen');
        if (form) {
            form.addEventListener('submit', guardar);
            porId('btnNuevoRegimen').addEventListener('click', vaciarFormulario);
        }
        cargar();
    });
})(typeof window !== 'undefined' ? window : globalThis);
