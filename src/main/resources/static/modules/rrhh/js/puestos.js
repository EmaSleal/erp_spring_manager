// ============================================================================
// PUESTOS.JS — RRHH Module
// ERP Orders Manager
// ============================================================================

let puestoModal = null;

document.addEventListener('DOMContentLoaded', function () {
    const modalEl = document.getElementById('puestoModal');
    if (modalEl) {
        puestoModal = new bootstrap.Modal(modalEl);
    }

    document.getElementById('btnNuevoPuesto')?.addEventListener('click', abrirModalNuevo);
});

// ── Modal helpers ─────────────────────────────────────────────────────────────

function abrirModalNuevo() {
    document.getElementById('puestoModalLabel').textContent = 'Nuevo Puesto';
    document.getElementById('puestoId').value           = '';
    document.getElementById('puestoNombre').value       = '';
    document.getElementById('puestoDescripcion').value  = '';
    document.getElementById('puestoSalarioBase').value  = '';
    document.getElementById('puestoTipoJornada').value  = '';
    document.getElementById('puestoCategoriaSalarial').value = '';
    document.getElementById('puestoDepartamentoId').value    = '';
    puestoModal.show();
}

function abrirModalEditar(btn) {
    document.getElementById('puestoModalLabel').textContent = 'Editar Puesto';
    document.getElementById('puestoId').value                    = btn.dataset.id       || '';
    document.getElementById('puestoNombre').value               = btn.dataset.nombre    || '';
    document.getElementById('puestoDescripcion').value          = btn.dataset.descripcion || '';
    document.getElementById('puestoSalarioBase').value          = btn.dataset.salario   || '';
    document.getElementById('puestoCategoriaSalarial').value    = btn.dataset.categoria || '';
    document.getElementById('puestoTipoJornada').value          = btn.dataset.jornada   || '';
    document.getElementById('puestoDepartamentoId').value       = btn.dataset.departamentoId || '';
    puestoModal.show();
}

// ── CRUD ─────────────────────────────────────────────────────────────────────

function guardarPuesto() {
    const id             = document.getElementById('puestoId').value;
    const nombre         = document.getElementById('puestoNombre').value.trim();
    const descripcion    = document.getElementById('puestoDescripcion').value.trim();
    const salarioBase    = document.getElementById('puestoSalarioBase').value;
    const tipoJornada    = document.getElementById('puestoTipoJornada').value || null;
    const categoriaSal   = document.getElementById('puestoCategoriaSalarial').value.trim() || null;
    const departamentoId = document.getElementById('puestoDepartamentoId').value;

    if (!nombre) {
        showToast('error', 'El nombre del puesto es requerido.');
        return;
    }
    if (!departamentoId) {
        showToast('error', 'Debe seleccionar un departamento.');
        return;
    }
    if (!salarioBase || parseFloat(salarioBase) < 0) {
        showToast('error', 'El salario base debe ser un valor positivo.');
        return;
    }

    const payload = {
        nombre,
        descripcion: descripcion || null,
        salarioBase: parseFloat(salarioBase),
        tipoJornada,
        categoriaSalarialMinima: categoriaSal,
        departamentoId: parseInt(departamentoId, 10)
    };

    if (id) {
        httpPost('/api/rrhh/puestos/' + id, payload, {
            successMessage: 'Puesto actualizado correctamente.',
            reloadOnSuccess: true
        });
    } else {
        httpPost('/api/rrhh/puestos', payload, {
            successMessage: 'Puesto creado correctamente.',
            reloadOnSuccess: true
        });
    }
}

function confirmarDesactivar(btn) {
    const id     = btn.dataset.id;
    const nombre = btn.dataset.nombre;

    showConfirmDialog(
        '¿Desactivar puesto?',
        'El puesto "' + nombre + '" quedará inactivo.',
        'Sí, desactivar'
    ).then(function (confirmed) {
        if (confirmed) {
            httpDelete('/api/rrhh/puestos/' + id, {
                successMessage: 'Puesto desactivado correctamente.',
                reloadOnSuccess: true
            });
        }
    });
}

// ── Cascade: load puestos by departamento (used by PR3 empleado form) ─────────

function cargarPuestosPorDepartamento(departamentoId, selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;

    select.innerHTML = '<option value="">Cargando...</option>';

    if (!departamentoId) {
        select.innerHTML = '<option value="">Seleccione un departamento primero</option>';
        return;
    }

    httpGet('/api/rrhh/puestos?departamentoId=' + departamentoId, {
        showLoading: false,
        onSuccess: function (data) {
            const puestos = data.data || data;
            select.innerHTML = '<option value="">Seleccione un puesto</option>';
            puestos.forEach(function (p) {
                const opt = document.createElement('option');
                opt.value       = p.id;
                opt.textContent = p.nombre;
                select.appendChild(opt);
            });
        },
        onError: function () {
            select.innerHTML = '<option value="">Error al cargar puestos</option>';
        }
    });
}

// ── Client-side filter ────────────────────────────────────────────────────────

function filtrarTabla() {
    const q    = (document.getElementById('searchInput')?.value || '').toLowerCase();
    const dept = (document.getElementById('filtroDepartamento')?.value || '').toLowerCase();
    const act  = document.getElementById('filtroActivo')?.value || '';
    const rows = document.querySelectorAll('#tablaPuestos tbody tr[data-nombre]');

    rows.forEach(function (row) {
        const nombre     = (row.dataset.nombre        || '').toLowerCase();
        const deptoVal   = (row.dataset.departamento  || '').toLowerCase();
        const activoVal  = row.dataset.activo;

        const ok = (!q    || nombre.includes(q))
                && (!dept  || deptoVal.includes(dept))
                && (!act   || activoVal === act);

        row.style.display = ok ? '' : 'none';
    });
}
