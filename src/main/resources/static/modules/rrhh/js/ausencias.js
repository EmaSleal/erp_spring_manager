/**
 * ausencias.js — RRHH Ausencias module
 * Uses httpGet / httpPost helpers from common.js — no raw fetch().
 */

document.addEventListener('DOMContentLoaded', () => {
    cargarAusencias();
});

/** Load absences — all employees when no filter, or filtered by employee. */
function cargarAusencias() {
    const empleadoId = document.getElementById('filtroEmpleado').value;
    const container = document.getElementById('ausenciasList');
    const url = empleadoId
        ? '/api/rrhh/ausencias/empleado/' + empleadoId
        : '/api/rrhh/ausencias';

    httpGet(url, {
        showLoading: true,
        onSuccess: (data) => {
            renderAusenciasTable(data.data, container);
        },
        onError: () => {
            container.innerHTML = '<div class="text-center py-4 text-danger">'
                + 'Error al cargar las ausencias.</div>';
        }
    });
}

/** Render absences as a Bootstrap table. */
function renderAusenciasTable(ausencias, container) {
    if (!ausencias || ausencias.length === 0) {
        container.innerHTML = '<div class="text-center py-5 text-muted">'
            + '<i class="fas fa-calendar-times fa-3x mb-3 d-block"></i>'
            + 'Este empleado no tiene ausencias registradas.</div>';
        return;
    }

    const canGestionar = typeof permisos !== 'undefined' && permisos.gestionar;

    let html = '<div class="table-responsive"><table class="table table-hover mb-0">'
        + '<thead class="table-light"><tr>'
        + '<th>Empleado</th><th>Tipo</th><th>Inicio</th><th>Fin</th><th>Goce</th><th>Estado</th><th>Acciones</th>'
        + '</tr></thead><tbody>';

    ausencias.forEach(a => {
        const aprobadaBadge = a.aprobada
            ? '<span class="badge bg-success">Aprobada</span>'
            : '<span class="badge bg-warning text-dark">Pendiente</span>';

        const goceBadge = a.conGoceSalario
            ? '<span class="badge bg-info text-dark">Con goce</span>'
            : '<span class="badge bg-secondary">Sin goce</span>';

        const empleadoNombre = (a.empleado && a.empleado.nombreCompleto)
            ? a.empleado.nombreCompleto
            : (a.empleadoNombre || '—');

        html += '<tr>'
            + '<td>' + empleadoNombre + '</td>'
            + '<td>' + (a.tipoAusencia || '-') + '</td>'
            + '<td>' + formatDate(a.fechaInicio) + '</td>'
            + '<td>' + formatDate(a.fechaFin) + '</td>'
            + '<td>' + goceBadge + '</td>'
            + '<td>' + aprobadaBadge + '</td>';

        let acciones = '';
        if (!a.aprobada) {
            if (canGestionar) {
                acciones += '<button class="btn btn-sm btn-outline-success me-1" '
                    + 'onclick="confirmarAprobar(' + a.id + ')">Aprobar</button>';
            }
            acciones += '<button class="btn btn-sm btn-outline-primary" '
                + 'onclick="confirmarAprobarJefe(' + a.id + ')">Aprobar (jefe)</button>';
        }
        html += '<td>' + (acciones || '—') + '</td>';

        html += '</tr>';
    });

    html += '</tbody></table></div>';
    container.innerHTML = html;
}

/** Register a new absence (called from form.html). */
function registrarAusencia() {
    const dto = {
        empleadoId:            getIntValue('empleadoId'),
        tipoAusencia:          getValue('tipoAusencia'),
        fechaInicio:           getValue('fechaInicio'),
        fechaFin:              getValue('fechaFin'),
        descripcion:           getValue('descripcion'),
        conGoceSalario:        getCheckValue('conGoceSalario'),
        justificada:           getCheckValue('justificada'),
        computaParaAguinaldo:  getCheckValue('computaParaAguinaldo'),
        computaAntiguedad:     getCheckValue('computaAntiguedad')
    };

    if (!dto.empleadoId || !dto.tipoAusencia || !dto.fechaInicio || !dto.fechaFin) {
        showToast('error', 'Complete los campos obligatorios.');
        return;
    }

    httpPost('/api/rrhh/ausencias/registrar', dto, {
        successMessage: 'Ausencia registrada exitosamente.',
        reloadOnSuccess: false,
        onSuccess: () => {
            window.location.href = '/rrhh/ausencias';
        }
    });
}

/** Confirm and approve an absence. */
function confirmarAprobar(ausenciaId) {
    showConfirmDialog(
        '¿Aprobar ausencia?',
        'La ausencia quedará marcada como aprobada.',
        'Sí, aprobar'
    ).then(confirmed => {
        if (!confirmed) return;
        aprobarAusencia(ausenciaId);
    });
}

function aprobarAusencia(ausenciaId) {
    // usuarioAprobadorId — in production, resolve from session; use 1 as placeholder
    const usuarioAprobadorId = 1;

    httpPost('/api/rrhh/ausencias/' + ausenciaId + '/aprobar', {
        usuarioAprobadorId: usuarioAprobadorId
    }, {
        successMessage: 'Ausencia aprobada exitosamente.',
        reloadOnSuccess: true
    });
}

/** Confirm and approve an absence as department boss (structural auth — no permission code). */
function confirmarAprobarJefe(ausenciaId) {
    showConfirmDialog(
        '¿Aprobar como jefe de departamento?',
        'Aprobás esta ausencia en tu rol de jefe del departamento.',
        'Sí, aprobar'
    ).then(confirmed => {
        if (!confirmed) return;
        httpPost('/api/rrhh/ausencias/' + ausenciaId + '/aprobar-jefe', {}, {
            successMessage: 'Ausencia aprobada exitosamente.',
            reloadOnSuccess: true
        });
    });
}

/** Reset filters and reload all. */
function limpiarFiltrosAusencia() {
    const filtroEmpleado = document.getElementById('filtroEmpleado');
    if (filtroEmpleado) filtroEmpleado.value = '';
    cargarAusencias();
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function getValue(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
}

function getIntValue(id) {
    const v = getValue(id);
    return v ? parseInt(v, 10) : null;
}

function getCheckValue(id) {
    const el = document.getElementById(id);
    return el ? el.checked : false;
}
