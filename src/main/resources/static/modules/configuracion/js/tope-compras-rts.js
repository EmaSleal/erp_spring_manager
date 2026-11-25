/**
 * T-056 (ADR-072): pantalla de parámetros por año del tope de compras del RTS (Configuración). Lista los años con su tope
 * (salario base x multiplicador), muestra el estado del año actual (acumulado, porcentaje y la advertencia de que solo
 * cuenta lo registrado en el ERP) y permite agregar o corregir un año. Todo se pinta con createElement y textContent: ningún
 * dato del servidor se interpreta como HTML. Las funciones puras se exponen en window.TopeComprasRtsUI para probarlas con
 * node --test sin DOM real.
 */
(function (global) {
    'use strict';

    const LARGO_NOTA = 300;
    const NUMERO = /^\d+(\.\d+)?$/;

    function numeroValido(valor) {
        return NUMERO.test(String(valor == null ? '' : valor).trim());
    }

    function decimalesOk(valor) {
        const partes = String(valor).trim().split('.');
        return partes.length < 2 || partes[1].length <= 2;
    }

    function vacio(valor) {
        return valor === null || valor === undefined || String(valor).trim() === '';
    }

    /** 1.234.567,89 (miles con punto, decimales con coma). */
    function formatoMonto(n) {
        const v = Number(n);
        if (!Number.isFinite(v)) return '—';
        const [entera, decimal] = Math.abs(v).toFixed(2).split('.');
        return (v < 0 ? '-' : '') + entera.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ',' + decimal;
    }

    function formatoNumero(n) {
        if (n === null || n === undefined || n === '') return '—';
        return formatoMonto(n);
    }

    /**
     * Valida el formulario antes de enviarlo (los valores llegan como texto de los inputs).
     * @returns {string|null} el mensaje del primer error, o null si es válido
     */
    function validarTope(t) {
        const f = t || {};
        const anio = String(f.anio == null ? '' : f.anio).trim();
        if (!/^\d{4}$/.test(anio) || Number(anio) < 2000 || Number(anio) > 2100) {
            return 'El año debe estar entre 2000 y 2100';
        }
        if (!numeroValido(f.salarioBase) || Number(f.salarioBase) <= 0 || !decimalesOk(f.salarioBase)) {
            return 'Indique el salario base (mayor que 0, con hasta 2 decimales)';
        }
        if (!vacio(f.multiplicador)
            && (!numeroValido(f.multiplicador) || Number(f.multiplicador) <= 0 || !decimalesOk(f.multiplicador))) {
            return 'El multiplicador debe ser mayor que 0, con hasta 2 decimales';
        }
        const mensajeUmbral = 'Los umbrales deben estar entre 0 y 100, y el de aviso menor que el de tope';
        for (const u of [f.umbralAviso, f.umbralTope]) {
            if (!vacio(u) && (!numeroValido(u) || Number(u) <= 0 || Number(u) > 100 || !decimalesOk(u))) {
                return mensajeUmbral;
            }
        }
        if (!vacio(f.umbralAviso) && !vacio(f.umbralTope) && Number(f.umbralAviso) >= Number(f.umbralTope)) {
            return mensajeUmbral;
        }
        if (f.nota != null && String(f.nota).length > LARGO_NOTA) {
            return 'La nota no puede exceder ' + LARGO_NOTA + ' caracteres';
        }
        return null;
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
     * @param {Array} lista parámetros por año tal como los da la API (con tope = salario base x multiplicador)
     * @param {{puedeGestionar: boolean, onEditar: Function}} opciones
     */
    function pintarTopes(cuerpo, lista, opciones, doc) {
        const d = doc || global.document;
        const op = opciones || {};
        cuerpo.replaceChildren();
        if (!lista || !lista.length) {
            const tr = d.createElement('tr');
            const td = celda(d, 'No hay topes de compras registrados', 'text-center text-muted py-3');
            td.setAttribute('colspan', '8');
            tr.appendChild(td);
            cuerpo.appendChild(tr);
            return;
        }
        lista.forEach((t) => {
            const tr = d.createElement('tr');
            tr.appendChild(celda(d, t.anio, 'fw-bold'));
            tr.appendChild(celda(d, formatoNumero(t.salarioBase), 'text-end'));
            tr.appendChild(celda(d, formatoNumero(t.multiplicador), 'text-end'));
            tr.appendChild(celda(d, formatoNumero(t.tope), 'text-end fw-bold'));
            tr.appendChild(celda(d, formatoNumero(t.umbralAviso) + ' % / ' + formatoNumero(t.umbralTope) + ' %', 'text-center'));
            tr.appendChild(celda(d, t.nota));
            tr.appendChild(celda(d, t.usuarioNombre));
            const acciones = d.createElement('td');
            acciones.className = 'text-center';
            if (op.puedeGestionar) {
                acciones.appendChild(boton(d, 'fas fa-edit', 'Editar el tope de ' + t.anio, 'btn-outline-primary',
                    () => op.onEditar && op.onEditar(t)));
            }
            tr.appendChild(acciones);
            cuerpo.appendChild(tr);
        });
    }

    /** Estado del año (respuesta de /api/tope-compras-rts/{anio}/estado) en una sola caja de texto. */
    function pintarEstado(caja, estado) {
        const e = estado || {};
        const lineas = [];
        if (!e.configurado) {
            lineas.push(e.mensaje || ('Tope de ' + e.anio + ' sin configurar'));
            caja.className = 'alert alert-secondary mb-0';
        } else {
            lineas.push('Acumulado ' + e.anio + ': ₡' + formatoMonto(e.acumulado) + ' de ₡' + formatoMonto(e.tope)
                + ' (' + String(formatoMonto(e.porcentaje)) + ' %)');
            if (e.mensaje) lineas.push(e.mensaje);
            caja.className = 'alert mb-0 ' + (e.nivel === 'TOPE' ? 'alert-danger' : e.nivel === 'AVISO' ? 'alert-warning' : 'alert-success');
        }
        if (e.advertencia) lineas.push(e.advertencia);
        caja.textContent = lineas.join(' ');
    }

    global.TopeComprasRtsUI = { validarTope, pintarTopes, pintarEstado, formatoMonto, LARGO_NOTA };

    // ── Página (solo con DOM real) ──────────────────────────────────────────────

    const doc = global.document;
    if (!doc || typeof doc.addEventListener !== 'function') return;

    let anioEnEdicion = null;
    let anioActual = null;

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
        anioEnEdicion = null;
        porId('formTope').reset();
        porId('topeAnio').disabled = false;
        porId('tituloFormTope').textContent = 'Agregar año';
        porId('topeError').textContent = '';
    }

    function textoONulo(id) {
        const v = porId(id).value.trim();
        return v === '' ? null : Number(v);
    }

    function editar(t) {
        anioEnEdicion = t.anio;
        porId('topeAnio').value = t.anio;
        porId('topeAnio').disabled = true;
        porId('topeSalario').value = t.salarioBase;
        porId('topeMultiplicador').value = t.multiplicador;
        porId('topeUmbralAviso').value = t.umbralAviso;
        porId('topeUmbralTope').value = t.umbralTope;
        porId('topeNota').value = t.nota || '';
        porId('tituloFormTope').textContent = 'Corregir el año ' + t.anio;
        porId('topeError').textContent = '';
    }

    async function cargarEstado() {
        const caja = porId('topeEstado');
        if (!caja || !anioActual) return;
        try {
            const resp = await fetch('/api/tope-compras-rts/' + encodeURIComponent(anioActual) + '/estado');
            const json = resp.ok ? await resp.json() : null;
            if (json && json.data) pintarEstado(caja, json.data);
        } catch (e) {
            caja.textContent = 'No se pudo calcular el estado del año.';
        }
    }

    async function cargar() {
        try {
            const resp = await fetch('/api/tope-compras-rts');
            const json = resp.ok ? await resp.json() : null;
            pintarTopes(porId('topeCuerpo'), (json && json.data) || [], { puedeGestionar: true, onEditar: editar }, doc);
        } catch (e) {
            toast('Error al cargar los topes de compras.', 'danger');
        }
        cargarEstado();
    }

    async function guardar(evento) {
        evento.preventDefault();
        const campos = {
            anio: porId('topeAnio').value,
            salarioBase: porId('topeSalario').value,
            multiplicador: porId('topeMultiplicador').value,
            umbralAviso: porId('topeUmbralAviso').value,
            umbralTope: porId('topeUmbralTope').value,
            nota: porId('topeNota').value
        };
        const error = porId('topeError');
        error.textContent = '';
        const invalido = validarTope(campos);
        if (invalido) { error.textContent = invalido; return; }
        const cuerpo = {
            anio: Number(campos.anio),
            salarioBase: Number(campos.salarioBase),
            multiplicador: textoONulo('topeMultiplicador'),
            umbralAviso: textoONulo('topeUmbralAviso'),
            umbralTope: textoONulo('topeUmbralTope'),
            nota: campos.nota
        };
        const btn = porId('btnGuardarTope');
        btn.disabled = true;
        try {
            const editando = anioEnEdicion !== null;
            const resp = await fetch(editando ? '/api/tope-compras-rts/' + encodeURIComponent(anioEnEdicion) : '/api/tope-compras-rts', {
                method: editando ? 'PUT' : 'POST',
                headers: cabeceras(),
                body: JSON.stringify(cuerpo)
            });
            const data = await resp.json().catch(() => ({}));
            if (resp.ok && data.success) { toast(data.message || 'Guardado.', 'success'); vaciarFormulario(); cargar(); }
            else error.textContent = data.message || 'No se pudo guardar el tope.';
        } catch (e) {
            error.textContent = 'Error de conexión. Intente nuevamente.';
        } finally {
            btn.disabled = false;
        }
    }

    doc.addEventListener('DOMContentLoaded', () => {
        const tabla = porId('tablaTope');
        if (!tabla) return;
        anioActual = tabla.getAttribute('data-anio');
        porId('formTope').addEventListener('submit', guardar);
        porId('btnNuevoTope').addEventListener('click', vaciarFormulario);
        cargar();
    });
})(typeof window !== 'undefined' ? window : globalThis);
