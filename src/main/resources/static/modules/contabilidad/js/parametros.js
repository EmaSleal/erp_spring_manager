/**
 * parametros.js — CRUD UI for ParametroContable
 * Depends on common.js (httpRequest, showConfirmDialog, showToast)
 */

async function cargarCuentas(selectId) {
    const select = document.getElementById(selectId);
    select.innerHTML = '<option value="">Cargando...</option>';
    try {
        const result = await httpRequest({
            url: '/contabilidad/cuentas/api/todas',
            method: 'GET',
            showLoading: false
        });
        const cuentas = Array.isArray(result) ? result : (result.data || []);
        select.innerHTML = '<option value="">Seleccione una cuenta...</option>' +
            cuentas.map(c =>
                `<option value="${c.idCuenta}">${c.codigo} — ${c.nombre}</option>`
            ).join('');
    } catch (e) {
        select.innerHTML = '<option value="">Error al cargar cuentas</option>';
    }
}

function abrirModalCrear() {
    document.getElementById('claveCrear').value = '';
    document.getElementById('descripcionCrear').value = '';
    cargarCuentas('cuentaContableIdCrear');
    new bootstrap.Modal(document.getElementById('modalCrear')).show();
}

async function guardarParametro() {
    const clave = document.getElementById('claveCrear').value.trim();
    const descripcion = document.getElementById('descripcionCrear').value.trim();
    const cuentaContableId = document.getElementById('cuentaContableIdCrear').value;

    if (!clave || !cuentaContableId) {
        showToast('warning', 'Clave y cuenta contable son obligatorios');
        return;
    }

    await httpRequest({
        url: '/api/contabilidad/parametros',
        method: 'POST',
        data: { clave, descripcion, cuentaContableId: parseInt(cuentaContableId) },
        reloadOnSuccess: true
    });
}

function abrirModalEditar(btn) {
    const { id, clave, descripcion, cuentaId } = btn.dataset;

    document.getElementById('idEditar').value = id;
    document.getElementById('claveEditar').value = clave;
    document.getElementById('descripcionEditar').value = descripcion || '';

    cargarCuentas('cuentaContableIdEditar').then(() => {
        document.getElementById('cuentaContableIdEditar').value = cuentaId;
    });

    new bootstrap.Modal(document.getElementById('modalEditar')).show();
}

async function actualizarParametro() {
    const id = document.getElementById('idEditar').value;
    const clave = document.getElementById('claveEditar').value.trim();
    const descripcion = document.getElementById('descripcionEditar').value.trim();
    const cuentaContableId = document.getElementById('cuentaContableIdEditar').value;

    if (!clave || !cuentaContableId) {
        showToast('warning', 'Clave y cuenta contable son obligatorios');
        return;
    }

    await httpRequest({
        url: `/api/contabilidad/parametros/${id}`,
        method: 'PUT',
        data: { clave, descripcion, cuentaContableId: parseInt(cuentaContableId) },
        reloadOnSuccess: true
    });
}

async function eliminarParametro(btn) {
    const { id, clave } = btn.dataset;

    const confirmed = await showConfirmDialog(
        '¿Eliminar parámetro?',
        `Se eliminará el parámetro "${clave}". Esta acción no se puede deshacer.`,
        'Sí, eliminar'
    );
    if (!confirmed) return;

    await httpRequest({
        url: `/api/contabilidad/parametros/${id}`,
        method: 'DELETE',
        reloadOnSuccess: true
    });
}
