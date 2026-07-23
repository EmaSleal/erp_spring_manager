// ============================================================================
// DEPARTAMENTOS.JS — RRHH Module
// ERP Orders Manager
// ============================================================================

let departamentoModal = null;

document.addEventListener('DOMContentLoaded', function () {
    const modalEl = document.getElementById('departamentoModal');
    if (modalEl) {
        departamentoModal = new bootstrap.Modal(modalEl);
    }

    document.getElementById('btnNuevoDepartamento')?.addEventListener('click', abrirModalNuevo);
});

// ── Modal helpers ─────────────────────────────────────────────────────────────

function abrirModalNuevo() {
    document.getElementById('departamentoModalLabel').textContent = 'Nuevo Departamento';
    document.getElementById('departamentoId').value = '';
    document.getElementById('departamentoNombre').value = '';
    document.getElementById('departamentoJefeId').value = '';
    departamentoModal.show();
}

function abrirModalEditar(btn) {
    const id     = btn.dataset.id;
    const nombre = btn.dataset.nombre;
    const jefeId = btn.dataset.jefeId || '';
    document.getElementById('departamentoModalLabel').textContent = 'Editar Departamento';
    document.getElementById('departamentoId').value   = id;
    document.getElementById('departamentoNombre').value = nombre;
    document.getElementById('departamentoJefeId').value = jefeId;
    departamentoModal.show();
}

// ── CRUD ─────────────────────────────────────────────────────────────────────

function guardarDepartamento() {
    const id     = document.getElementById('departamentoId').value;
    const nombre = document.getElementById('departamentoNombre').value.trim();
    const jefeIdRaw = document.getElementById('departamentoJefeId').value;
    const jefeId = jefeIdRaw ? parseInt(jefeIdRaw, 10) : null;

    if (!nombre) {
        showToast('error', 'El nombre del departamento es requerido.');
        return;
    }

    const payload = { nombre, jefeId };

    if (id) {
        httpPost('/api/rrhh/departamentos/' + id, payload, {
            successMessage: 'Departamento actualizado correctamente.',
            reloadOnSuccess: true
        });
    } else {
        httpPost('/api/rrhh/departamentos', payload, {
            successMessage: 'Departamento creado correctamente.',
            reloadOnSuccess: true
        });
    }
}

function confirmarDesactivar(btn) {
    const id     = btn.dataset.id;
    const nombre = btn.dataset.nombre;

    showConfirmDialog(
        '¿Desactivar departamento?',
        'El departamento "' + nombre + '" quedará inactivo. Esta acción no se puede revertir desde aquí.',
        'Sí, desactivar'
    ).then(function (confirmed) {
        if (confirmed) {
            httpDelete('/api/rrhh/departamentos/' + id, {
                successMessage: 'Departamento desactivado correctamente.',
                reloadOnSuccess: true
            });
        }
    });
}

// ── Client-side filter ────────────────────────────────────────────────────────

function filtrarTabla() {
    const q      = (document.getElementById('searchInput')?.value || '').toLowerCase();
    const activo = document.getElementById('filtroActivo')?.value || '';
    const rows   = document.querySelectorAll('#tablaDepartamentos tbody tr[data-nombre]');

    rows.forEach(function (row) {
        const nombre     = (row.dataset.nombre || '').toLowerCase();
        const activoVal  = row.dataset.activo;

        const matchNombre = !q || nombre.includes(q);
        const matchActivo = !activo || activoVal === activo;

        row.style.display = matchNombre && matchActivo ? '' : 'none';
    });
}
