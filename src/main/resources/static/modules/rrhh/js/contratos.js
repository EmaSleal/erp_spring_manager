/**
 * contratos.js — RRHH Contratos module
 * Uses httpGet / httpPost helpers from common.js — no raw fetch().
 */

document.addEventListener('DOMContentLoaded', () => {
    filtrarContratos();
});

/** Load contracts — all employees when no filter, or filtered by employee. */
function filtrarContratos() {
    const empleadoId = document.getElementById('filtroEmpleado').value;
    const container = document.getElementById('contratosList');
    const url = empleadoId
        ? '/api/rrhh/contratos/empleado/' + empleadoId
        : '/api/rrhh/contratos';

    httpGet(url, {
        showLoading: true,
        onSuccess: (data) => {
            renderContratosTable(data.data, container);
        },
        onError: () => {
            container.innerHTML = '<div class="text-center py-4 text-danger">'
                + 'Error al cargar los contratos.</div>';
        }
    });
}

/** Render contracts as a Bootstrap table. */
function renderContratosTable(contratos, container) {
    if (!contratos || contratos.length === 0) {
        container.innerHTML = '<div class="text-center py-5 text-muted">'
            + '<i class="fas fa-file-contract fa-3x mb-3 d-block"></i>'
            + 'Este empleado no tiene contratos registrados.</div>';
        return;
    }

    let html = '<div class="table-responsive"><table class="table table-hover mb-0">'
        + '<thead class="table-light"><tr>'
        + '<th>Tipo</th><th>Cargo</th><th>Inicio</th><th>Fin</th>'
        + '<th>Salario Bruto</th><th>Estado</th>';

    if (typeof permisos !== 'undefined' && permisos.gestionar) {
        html += '<th>Acciones</th>';
    }

    html += '</tr></thead><tbody>';

    contratos.forEach(c => {
        const activo = c.activo
            ? '<span class="badge bg-success">Activo</span>'
            : '<span class="badge bg-secondary">Inactivo</span>';

        html += '<tr>'
            + '<td>' + (c.tipoContrato || '-') + '</td>'
            + '<td>' + (c.cargoContratado || '-') + '</td>'
            + '<td>' + formatDate(c.fechaInicio) + '</td>'
            + '<td>' + (c.fechaFin ? formatDate(c.fechaFin) : 'Indefinido') + '</td>'
            + '<td>' + formatCurrency(c.salarioBruto) + '</td>'
            + '<td>' + activo + '</td>';

        if (typeof permisos !== 'undefined' && permisos.gestionar && c.activo) {
            html += '<td><button class="btn btn-sm btn-outline-danger" '
                + 'onclick="confirmarTerminar(' + c.id + ')">Terminar</button></td>';
        } else if (typeof permisos !== 'undefined' && permisos.gestionar) {
            html += '<td>—</td>';
        }

        html += '</tr>';
    });

    html += '</tbody></table></div>';
    container.innerHTML = html;
}

/** Save a new contract (called from form.html). */
function guardarContrato() {
    const dto = {
        empleadoId:      getIntValue('empleadoId'),
        tipoContrato:    getValue('tipoContrato'),
        cargoContratado: getValue('cargoContratado'),
        jornada:         getValue('jornada'),
        fechaInicio:     getValue('fechaInicio'),
        fechaFin:        getValue('fechaFin') || null,
        salarioBruto:    getFloatValue('salarioBruto')
    };

    if (!dto.empleadoId || !dto.tipoContrato || !dto.fechaInicio || !dto.salarioBruto) {
        showToast('error', 'Complete los campos obligatorios.');
        return;
    }

    httpPost('/api/rrhh/contratos/guardar', dto, {
        successMessage: 'Contrato creado exitosamente.',
        reloadOnSuccess: false,
        onSuccess: () => {
            window.location.href = '/rrhh/contratos';
        }
    });
}

/** Show Swal form to collect termination data, then POST. */
function confirmarTerminar(contratoId) {
    const today = new Date().toISOString().slice(0, 10);
    Swal.fire({
        title: 'Terminar contrato',
        html:
            '<div class="mb-3 text-start">'
            + '<label class="form-label fw-semibold">Causa de terminación <span class="text-danger">*</span></label>'
            + '<select id="swal-causa" class="form-select">'
            + '<option value="">Seleccionar...</option>'
            + '<option value="RENUNCIA_VOLUNTARIA">Renuncia voluntaria</option>'
            + '<option value="DESPIDO_SIN_CAUSA">Despido sin causa</option>'
            + '<option value="DESPIDO_CON_CAUSA">Despido con causa</option>'
            + '<option value="VENCIMIENTO_PLAZO">Vencimiento de plazo</option>'
            + '<option value="MUTUO_ACUERDO">Mutuo acuerdo</option>'
            + '<option value="CASO_FORTUITO">Caso fortuito</option>'
            + '<option value="MUERTE_TRABAJADOR">Muerte del trabajador</option>'
            + '</select>'
            + '</div>'
            + '<div class="mb-3 text-start">'
            + '<label class="form-label fw-semibold">Fecha de terminación <span class="text-danger">*</span></label>'
            + '<input id="swal-fecha" type="date" class="form-control" value="' + today + '" />'
            + '</div>'
            + '<div class="mb-1 text-start">'
            + '<label class="form-label fw-semibold">Descripción <small class="text-muted">(opcional)</small></label>'
            + '<textarea id="swal-descripcion" class="form-control" rows="2" placeholder="Motivo adicional..."></textarea>'
            + '</div>',
        showCancelButton: true,
        confirmButtonText: 'Terminar contrato',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        focusConfirm: false,
        preConfirm: () => {
            const causa = document.getElementById('swal-causa').value;
            const fecha = document.getElementById('swal-fecha').value;
            if (!causa) {
                Swal.showValidationMessage('Seleccione una causa de terminación.');
                return false;
            }
            if (!fecha) {
                Swal.showValidationMessage('Ingrese la fecha de terminación.');
                return false;
            }
            return {
                causa: causa,
                fechaTerminacion: fecha,
                descripcion: document.getElementById('swal-descripcion').value.trim()
            };
        }
    }).then(result => {
        if (!result.isConfirmed) return;
        httpPost('/api/rrhh/contratos/' + contratoId + '/terminar', result.value, {
            successMessage: 'Contrato terminado exitosamente.',
            reloadOnSuccess: true
        });
    });
}

/** Reset filters. */
function limpiarFiltros() {
    const filtroEmpleado = document.getElementById('filtroEmpleado');
    if (filtroEmpleado) filtroEmpleado.value = '';
    const container = document.getElementById('contratosList');
    if (container) {
        container.innerHTML = '<div class="text-center py-5 text-muted">'
            + '<i class="fas fa-info-circle me-2"></i>Seleccione un empleado para ver sus contratos.</div>';
    }
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

function getFloatValue(id) {
    const v = getValue(id);
    return v ? parseFloat(v) : null;
}
