// ========================================
// Búsqueda de códigos CAByS en el formulario del artículo maestro (ADR-040)
// Endpoint: GET /api/articulos/cabys/buscar?q=&top=
// Todo texto que viene del catálogo se inserta con textContent (nunca como HTML).
// ========================================

const CABYS_MSG_ERROR_GENERICO = 'No se pudo completar la búsqueda de CAByS. Intente nuevamente.';

function cabysEl(id) {
    return document.getElementById(id);
}

function cabysCrear(tag, className, texto) {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (texto !== undefined && texto !== null) el.textContent = String(texto);
    return el;
}

/** Muestra un mensaje inline (tipo: info | warning | danger) en el contenedor dado. */
function cabysMostrarMensaje(contenedorId, tipo, texto) {
    const cont = cabysEl(contenedorId);
    if (!cont) return;
    cont.replaceChildren();
    if (!texto) {
        cont.style.display = 'none';
        return;
    }
    const alerta = cabysCrear('div', 'alert alert-' + tipo + ' py-2 mb-0', texto);
    alerta.setAttribute('role', 'alert');
    cont.appendChild(alerta);
    cont.style.display = 'block';
}

function mostrarErrorCabys(texto) {
    cabysMostrarMensaje('cabysMensaje', 'danger', texto);
}

/**
 * Buscar códigos CAByS (catálogo de Hacienda) a través del servidor.
 * Los errores se muestran inline y no bloquean el formulario.
 */
async function buscarCabys() {
    const input = cabysEl('cabysBusqueda');
    const termino = input ? String(input.value || '').trim() : '';
    cabysMostrarMensaje('cabysAviso', 'warning', '');

    if (!termino) {
        mostrarResultadosCabys([]);
        cabysMostrarMensaje('cabysMensaje', 'warning', 'Ingrese un término de búsqueda.');
        return;
    }

    const btn = cabysEl('btnBuscarCabys');
    if (btn) btn.disabled = true;
    cabysMostrarMensaje('cabysMensaje', 'info', 'Buscando...');

    try {
        const resp = await fetch('/api/articulos/cabys/buscar?q=' + encodeURIComponent(termino) + '&top=10', {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        });
        let cuerpo = null;
        try {
            cuerpo = await resp.json();
        } catch (e) {
            cuerpo = null;
        }
        if (!resp.ok || !cuerpo || cuerpo.success === false) {
            mostrarResultadosCabys([]);
            mostrarErrorCabys(cuerpo && cuerpo.message ? cuerpo.message : CABYS_MSG_ERROR_GENERICO);
            return;
        }
        const items = Array.isArray(cuerpo.data) ? cuerpo.data : [];
        mostrarResultadosCabys(items);
        if (items.length === 0) {
            cabysMostrarMensaje('cabysMensaje', 'info', 'No se encontraron códigos CAByS para el término buscado.');
        } else {
            cabysMostrarMensaje('cabysMensaje', 'info', '');
        }
    } catch (e) {
        mostrarResultadosCabys([]);
        mostrarErrorCabys(CABYS_MSG_ERROR_GENERICO);
    } finally {
        if (btn) btn.disabled = false;
    }
}

/** Dibuja la lista de resultados sin interpolar HTML. */
function mostrarResultadosCabys(cabys) {
    const divResultados = cabysEl('cabysResultados');
    const listGroup = cabysEl('cabysListGroup');
    if (!listGroup) return;
    listGroup.replaceChildren();

    if (!cabys || cabys.length === 0) {
        if (divResultados) divResultados.style.display = 'none';
        return;
    }

    cabys.forEach(item => {
        const elemento = cabysCrear('a', 'list-group-item list-group-item-action');
        elemento.href = '#';
        elemento.addEventListener('click', (e) => {
            e.preventDefault();
            seleccionarCabys(item);
        });

        const fila = cabysCrear('div', 'd-flex w-100 justify-content-between');
        const titulo = cabysCrear('h6', 'mb-1');
        titulo.appendChild(cabysCrear('span', 'badge bg-warning text-dark me-1', item.codigo));
        titulo.appendChild(cabysCrear('span', '', item.descripcion));
        fila.appendChild(titulo);
        if (item.impuesto !== null && item.impuesto !== undefined) {
            fila.appendChild(cabysCrear('small', 'text-muted', item.impuesto + '% IVA'));
        }
        elemento.appendChild(fila);

        const categorias = Array.isArray(item.categorias) ? item.categorias.join(' > ') : item.categorias;
        if (categorias) {
            elemento.appendChild(cabysCrear('small', 'text-muted', categorias));
        }
        listGroup.appendChild(elemento);
    });

    if (divResultados) divResultados.style.display = 'block';
}

/** Porcentaje efectivo actual del formulario (0 si no está gravado). */
function cabysImpuestoActual() {
    const gravado = cabysEl('gravado');
    const pct = cabysEl('porcentajeImpuesto');
    if (!gravado || !gravado.checked) return 0;
    const n = Number(pct ? pct.value : 0);
    return Number.isFinite(n) ? n : 0;
}

function cabysFormatoImpuesto(valor) {
    return valor > 0 ? valor + '% (gravado)' : '0% (exento)';
}

/**
 * Llena codigoCabys y descripcionCabys con el resultado elegido. Si el impuesto del catálogo difiere del
 * del artículo, lo aplica a gravado/porcentajeImpuesto y avisa con ambos valores. No guarda nada.
 */
function seleccionarCabys(cabys) {
    cabysEl('codigoCabys').value = cabys.codigo;
    cabysEl('descripcionCabys').value = cabys.descripcion;
    cabysMostrarMensaje('cabysAviso', 'warning', '');
    cabysMostrarMensaje('cabysMensaje', 'info', '');

    if (cabys.impuesto !== null && cabys.impuesto !== undefined) {
        const nuevo = Number(cabys.impuesto);
        const anterior = cabysImpuestoActual();
        if (Number.isFinite(nuevo) && nuevo !== anterior) {
            cabysEl('gravado').checked = nuevo > 0;
            cabysEl('porcentajeImpuesto').value = nuevo > 0 ? nuevo.toFixed(2) : '0';
            if (typeof toggleImpuesto === 'function') toggleImpuesto();
            cabysMostrarMensaje('cabysAviso', 'warning',
                'El impuesto del artículo cambió de ' + cabysFormatoImpuesto(anterior) + ' a ' +
                cabysFormatoImpuesto(nuevo) + ' según el catálogo. Guarde el artículo para conservar el cambio.');
        }
    }

    cabysEl('cabysResultados').style.display = 'none';
    cabysEl('cabysBusqueda').value = '';
}

function cabysIniciar() {
    const input = cabysEl('cabysBusqueda');
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarCabys();
            }
        });
    }
    const btn = cabysEl('btnBuscarCabys');
    if (btn) btn.addEventListener('click', () => buscarCabys());
}

if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') {
    document.addEventListener('DOMContentLoaded', cabysIniciar);
}
