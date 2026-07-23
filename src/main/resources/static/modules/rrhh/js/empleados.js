/**
 * empleados.js — RRHH Empleados module
 *
 * Handles:
 * - Table filtering (client-side by name/cedula/departamento/estado)
 * - Departamento → Puesto cascade via httpGet
 * - Save/update employee via httpPost
 * - Dar de baja flow via showConfirmDialog + httpPost
 */

// ── Table filtering ───────────────────────────────────────────────────────────

function filtrarTabla() {
    const search = (document.getElementById('searchInput')?.value || '').toLowerCase();
    const deptFilter = (document.getElementById('filtroDepartamento')?.value || '').toLowerCase();
    const estadoFilter = (document.getElementById('filtroEstado')?.value || '').toLowerCase();

    const rows = document.querySelectorAll('#tablaEmpleados tbody tr[data-nombre]');
    rows.forEach(row => {
        const nombre = (row.dataset.nombre || '').toLowerCase();
        const cedula = (row.querySelector('td:first-child')?.textContent || '').toLowerCase();
        const depto = (row.dataset.departamento || '').toLowerCase();
        const estado = (row.dataset.estado || '').toLowerCase();

        const matchSearch = !search || nombre.includes(search) || cedula.includes(search);
        const matchDept = !deptFilter || depto === deptFilter;
        const matchEstado = !estadoFilter || estado === estadoFilter.toUpperCase().replace(' ', '_');

        row.style.display = (matchSearch && matchDept && matchEstado) ? '' : 'none';
    });
}

// ── Cascade: Departamento → Puesto ───────────────────────────────────────────

function cargarPuestos() {
    const deptId = document.getElementById('departamentoId')?.value;
    const puestoSelect = document.getElementById('puestoId');
    if (!puestoSelect) return;

    if (!deptId) {
        puestoSelect.innerHTML = '<option value="">Seleccionar departamento primero</option>';
        return;
    }

    httpGet('/api/rrhh/puestos?departamentoId=' + deptId + '&soloActivos=true', {
        showLoading: false,
        onSuccess: (data) => {
            const puestos = data.data || data;
            puestoSelect.innerHTML = '<option value="">Seleccionar puesto</option>';
            puestos.forEach(p => {
                const option = document.createElement('option');
                option.value = p.id;
                option.textContent = p.nombre;
                if (puestoActualId && p.id == puestoActualId) {
                    option.selected = true;
                }
                puestoSelect.appendChild(option);
            });
        },
        onError: () => {
            puestoSelect.innerHTML = '<option value="">Error al cargar puestos</option>';
        }
    });
}

// ── Save / Update employee ────────────────────────────────────────────────────

function guardarEmpleado() {
    const payload = {
        id: empleadoId || null,
        cedula: document.getElementById('cedula')?.value?.trim(),
        nombre: document.getElementById('nombre')?.value?.trim(),
        primerApellido: document.getElementById('primerApellido')?.value?.trim(),
        segundoApellido: document.getElementById('segundoApellido')?.value?.trim() || null,
        genero: document.getElementById('genero')?.value || null,
        estadoCivil: document.getElementById('estadoCivil')?.value || null,
        fechaNacimiento: document.getElementById('fechaNacimiento')?.value || null,
        email: document.getElementById('email')?.value?.trim() || null,
        telefono: document.getElementById('telefono')?.value?.trim() || null,
        direccion: document.getElementById('direccion')?.value?.trim() || null,
        departamentoId: document.getElementById('departamentoId')?.value || null,
        puestoId: document.getElementById('puestoId')?.value || null,
        fechaIngreso: document.getElementById('fechaIngreso')?.value || null,
        numeroAseguradoCCSS: document.getElementById('numeroAseguradoCCSS')?.value?.trim() || null,
        operadoraPensionesROP: document.getElementById('operadoraPensionesROP')?.value?.trim() || null,
        hijosCargaFamiliar: parseInt(document.getElementById('hijosCargaFamiliar')?.value || '0'),
        conyugeCargaFamiliar: document.getElementById('conyugeCargaFamiliar')?.checked || false,
        porcentajeSolidarista: document.getElementById('porcentajeSolidarista')?.value || null,
        montoPensionAlimentaria: document.getElementById('montoPensionAlimentaria')?.value || null,
        banco: document.getElementById('banco')?.value?.trim() || null,
        cuentaBancaria: document.getElementById('cuentaBancaria')?.value?.trim() || null,
        contactoEmergenciaNombre: document.getElementById('contactoEmergenciaNombre')?.value?.trim() || null,
        contactoEmergenciaTelefono: document.getElementById('contactoEmergenciaTelefono')?.value?.trim() || null,
        contactoEmergenciaRelacion: document.getElementById('contactoEmergenciaRelacion')?.value?.trim() || null,
        usuarioId: document.getElementById('usuarioId')?.value
            ? parseInt(document.getElementById('usuarioId').value)
            : null
    };

    // Client-side required validation
    if (!payload.cedula || !payload.nombre || !payload.primerApellido ||
        !payload.departamentoId || !payload.puestoId || !payload.fechaIngreso) {
        showToast('error', 'Por favor complete los campos obligatorios.');
        return;
    }

    if (empleadoId) {
        httpPost('/api/rrhh/empleados/' + empleadoId + '/actualizar', payload, {
            successMessage: 'Empleado actualizado correctamente',
            reloadOnSuccess: true
        });
    } else {
        httpPost('/api/rrhh/empleados/guardar', payload, {
            successMessage: 'Empleado creado correctamente',
            onSuccess: (data) => {
                const newId = data.data;
                if (newId) {
                    window.location.href = '/rrhh/empleados/' + newId;
                } else {
                    window.location.href = '/rrhh/empleados';
                }
            }
        });
    }
}

// ── Dar de baja ───────────────────────────────────────────────────────────────

function darDeBaja(id) {
    const modal = new bootstrap.Modal(document.getElementById('modalBaja'));
    modal.show();
    window._bajaEmpleadoId = id;
}

function confirmarBaja() {
    const id = window._bajaEmpleadoId || empleadoId;
    const fechaSalida = document.getElementById('fechaSalidaBaja')?.value;
    const motivo = document.getElementById('motivoBaja')?.value?.trim();

    if (!fechaSalida || !motivo) {
        showToast('error', 'La fecha de salida y el motivo son requeridos.');
        return;
    }

    showConfirmDialog(
        '¿Confirmar baja del empleado?',
        'Esta acción no se puede deshacer. El empleado quedará inactivo.',
        'Sí, dar de baja'
    ).then(confirmed => {
        if (!confirmed) return;

        const modal = bootstrap.Modal.getInstance(document.getElementById('modalBaja'));
        if (modal) modal.hide();

        httpPost('/api/rrhh/empleados/' + id + '/baja', { fechaSalida, motivo }, {
            successMessage: 'Empleado dado de baja correctamente',
            reloadOnSuccess: true
        });
    });
}

// ── Initialization ────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    // If editing, pre-load puestos for the already-selected departamento
    const deptSelect = document.getElementById('departamentoId');
    if (deptSelect && deptSelect.value && typeof puestoActualId !== 'undefined') {
        cargarPuestos();
    }
});
