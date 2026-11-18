// ============================================================================
// CLIENTE-HACIENDA.JS - Actividades económicas de «Verificar en Hacienda» (T-038, ADR-044 §5)
// Pinta las actividades que devuelve /fe/ae para elegir una y copiarla a codigoActividad. No guarda nada.
// Todo texto del servidor se asigna con textContent (nunca innerHTML).
// ============================================================================

function textoActividadHacienda(a) {
    const partes = [];
    if (a.tipo === 'P') partes.push('Principal');
    else if (a.tipo === 'S') partes.push('Secundaria');
    if (a.estado === 'A') partes.push('Activa');
    else if (a.estado === 'I') partes.push('Inactiva');
    const detalle = partes.length ? ' (' + partes.join(', ') + ')' : '';
    return String(a.codigo) + (a.descripcion ? ' - ' + a.descripcion : '') + detalle;
}

/**
 * Pinta las actividades como opciones de radio dentro de contenedor. Con exactamente una actividad activa (estado «A»)
 * se preselecciona. Devuelve la cantidad de actividades con código que se pintaron.
 */
function pintarActividadesHacienda(contenedor, actividades, doc) {
    if (!contenedor) return 0;
    const d = doc || document;
    contenedor.textContent = '';
    const lista = (Array.isArray(actividades) ? actividades : [])
        .filter(function (a) { return a && a.codigo != null && String(a.codigo).trim() !== ''; });
    if (lista.length === 0) {
        const vacio = d.createElement('div');
        vacio.textContent = 'Hacienda no informa actividades económicas para esta identificación.';
        contenedor.appendChild(vacio);
        return 0;
    }
    const activas = lista.filter(function (a) { return a.estado === 'A'; });
    const preseleccionada = activas.length === 1 ? activas[0] : null;
    lista.forEach(function (a, i) {
        const fila = d.createElement('div');
        fila.className = 'form-check';
        const radio = d.createElement('input');
        radio.type = 'radio';
        radio.name = 'actividadHacienda';
        radio.id = 'actividadHacienda' + i;
        radio.className = 'form-check-input';
        radio.value = String(a.codigo).trim();
        radio.checked = a === preseleccionada;
        const etiqueta = d.createElement('label');
        etiqueta.className = 'form-check-label';
        etiqueta.htmlFor = radio.id;
        etiqueta.textContent = textoActividadHacienda(a);
        fila.appendChild(radio);
        fila.appendChild(etiqueta);
        contenedor.appendChild(fila);
    });
    return lista.length;
}

/** Código de la actividad elegida, o null si no hay ninguna. */
function actividadHaciendaElegida(contenedor) {
    if (!contenedor) return null;
    const radios = contenedor.querySelectorAll('input[type="radio"]');
    for (let i = 0; i < radios.length; i++) {
        if (radios[i].checked) return radios[i].value;
    }
    return null;
}

/** Copia la actividad elegida al campo codigoActividad; no guarda. Devuelve true si copió. */
function copiarActividadHacienda(contenedor, campo) {
    const codigo = actividadHaciendaElegida(contenedor);
    if (!codigo || !campo) return false;
    campo.value = codigo;
    return true;
}
