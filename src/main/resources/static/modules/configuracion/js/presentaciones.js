// ========================================
// Mantenimiento de presentaciones (ADR-041, T-034)
// API: /api/configuracion/presentaciones y /unidades-fe
// Nombres y unidades se insertan con textContent (nunca como HTML).
// ========================================

const PRES_API = '/api/configuracion/presentaciones';
let presentaciones = [];
let unidadesFe = [];
let presModal = null;

function presEl(id) {
    return document.getElementById(id);
}

function presCelda(texto, className) {
    const td = document.createElement('td');
    if (className) td.className = className;
    td.textContent = texto === null || texto === undefined ? '' : String(texto);
    return td;
}

function presMostrarMensaje(id, texto) {
    const el = presEl(id);
    if (!el) return;
    el.textContent = texto || '';
    el.style.display = texto ? 'block' : 'none';
}

/** Mensaje de un cuerpo de error del servidor (400 con message fijo) o uno genérico. */
function presMensajeError(cuerpo, generico) {
    return cuerpo && cuerpo.message ? cuerpo.message : generico;
}

/** Dibuja la tabla de presentaciones. */
function renderPresentaciones(lista) {
    const tbody = presEl('presentacionesBody');
    tbody.replaceChildren();
    lista.forEach(p => {
        const tr = document.createElement('tr');
        tr.appendChild(presCelda(p.nombre));
        tr.appendChild(presCelda(p.empacado ? 'Sí' : 'No', 'text-center'));
        // codigo_unidad_fe solo es relevante cuando no es empacado
        tr.appendChild(presCelda(p.empacado ? '—' : (p.codigoUnidadFE || '')));
        const acciones = document.createElement('td');
        acciones.className = 'text-center';
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-sm btn-outline-primary';
        btn.title = 'Editar presentación';
        const icono = document.createElement('i');
        icono.className = 'fas fa-edit';
        btn.appendChild(icono);
        btn.addEventListener('click', () => abrirPresentacion(p));
        acciones.appendChild(btn);
        tr.appendChild(acciones);
        tbody.appendChild(tr);
    });
}

/** La unidad FE solo se pide (y es obligatoria) cuando la presentación NO es empacada. */
function actualizarUnidadFeVisible() {
    const empacado = presEl('presentacionEmpacado').checked;
    presEl('divUnidadFe').style.display = empacado ? 'none' : 'block';
}

function llenarUnidadesFe(unidades) {
    const select = presEl('presentacionUnidadFe');
    select.replaceChildren();
    const vacio = document.createElement('option');
    vacio.value = '';
    vacio.textContent = 'Seleccione...';
    select.appendChild(vacio);
    unidades.forEach(u => {
        const opt = document.createElement('option');
        opt.value = u;
        opt.textContent = u;
        select.appendChild(opt);
    });
}

function abrirPresentacion(p) {
    presMostrarMensaje('presentacionModalError', '');
    presEl('presentacionModalTitle').textContent = p ? 'Editar presentación' : 'Nueva presentación';
    presEl('presentacionId').value = p ? p.idPresentacion : '';
    presEl('presentacionNombre').value = p ? (p.nombre || '') : '';
    presEl('presentacionEmpacado').checked = p ? !!p.empacado : true;
    presEl('presentacionUnidadFe').value = p && p.codigoUnidadFE ? p.codigoUnidadFE : '';
    actualizarUnidadFeVisible();
    presModal.show();
}

/** Arma el cuerpo a enviar; null en codigoUnidadFE cuando es empacado. */
function datosPresentacionForm() {
    const empacado = presEl('presentacionEmpacado').checked;
    const unidad = presEl('presentacionUnidadFe').value;
    return {
        nombre: presEl('presentacionNombre').value.trim(),
        empacado: empacado,
        codigoUnidadFE: empacado ? null : (unidad || null)
    };
}

async function cargarPresentaciones() {
    try {
        const resp = await fetch(PRES_API, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
        const cuerpo = await resp.json().catch(() => null);
        if (!resp.ok || !cuerpo || cuerpo.success === false) {
            presMostrarMensaje('presentacionesError', presMensajeError(cuerpo, 'No se pudieron cargar las presentaciones.'));
            return;
        }
        presentaciones = cuerpo.data || [];
        presMostrarMensaje('presentacionesError', '');
        renderPresentaciones(presentaciones);
    } catch (e) {
        presMostrarMensaje('presentacionesError', 'No se pudieron cargar las presentaciones.');
    }
}

async function cargarUnidadesFe() {
    try {
        const resp = await fetch(PRES_API + '/unidades-fe', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
        const cuerpo = await resp.json().catch(() => null);
        if (resp.ok && cuerpo && Array.isArray(cuerpo.data)) {
            unidadesFe = cuerpo.data;
            llenarUnidadesFe(unidadesFe);
        } else {
            presMostrarMensaje('presentacionesError', 'No se pudo cargar la lista de unidades de medida.');
        }
    } catch (e) {
        presMostrarMensaje('presentacionesError', 'No se pudo cargar la lista de unidades de medida.');
    }
}

async function guardarPresentacion() {
    presMostrarMensaje('presentacionModalError', '');
    const datos = datosPresentacionForm();
    if (!datos.nombre) {
        presMostrarMensaje('presentacionModalError', 'El nombre es obligatorio.');
        return;
    }
    if (!datos.empacado && !datos.codigoUnidadFE) {
        presMostrarMensaje('presentacionModalError', 'Seleccione la unidad de medida para una presentación no empacada.');
        return;
    }
    const id = presEl('presentacionId').value;
    try {
        const resp = await fetch(id ? PRES_API + '/' + encodeURIComponent(id) : PRES_API, {
            method: id ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(datos)
        });
        const cuerpo = await resp.json().catch(() => null);
        if (!resp.ok || !cuerpo || cuerpo.success === false) {
            presMostrarMensaje('presentacionModalError', presMensajeError(cuerpo, 'No se pudo guardar la presentación.'));
            return;
        }
        presModal.hide();
        await cargarPresentaciones();
    } catch (e) {
        presMostrarMensaje('presentacionModalError', 'No se pudo guardar la presentación.');
    }
}

function presIniciar() {
    const modalEl = presEl('presentacionModal');
    if (modalEl && typeof bootstrap !== 'undefined') presModal = new bootstrap.Modal(modalEl);
    presEl('btnNuevaPresentacion').addEventListener('click', () => abrirPresentacion(null));
    presEl('btnGuardarPresentacion').addEventListener('click', guardarPresentacion);
    presEl('presentacionEmpacado').addEventListener('change', actualizarUnidadFeVisible);
    cargarUnidadesFe();
    cargarPresentaciones();
}

if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') {
    document.addEventListener('DOMContentLoaded', presIniciar);
}
