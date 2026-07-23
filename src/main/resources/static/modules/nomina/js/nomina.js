/**
 * nomina.js — Nómina module
 * Uses httpGet / httpPost helpers from common.js — no raw fetch().
 */

// ── lista.html ────────────────────────────────────────────────────────────────

/** Create a new payroll run (called from the modal in lista.html). */
function crearNomina() {
    const periodoInicio = document.getElementById('periodoInicio')?.value;
    const periodoFin    = document.getElementById('periodoFin')?.value;
    const fechaPago     = document.getElementById('fechaPago')?.value;
    const tipo          = document.getElementById('tipoNomina')?.value;

    if (!periodoInicio || !periodoFin || !fechaPago || !tipo) {
        showToast('error', 'Complete todos los campos obligatorios.');
        return;
    }

    if (periodoFin < periodoInicio) {
        showToast('error', 'La fecha fin debe ser mayor o igual al período inicio.');
        return;
    }

    httpPost('/api/nomina', { periodoInicio, periodoFin, fechaPago, tipo }, {
        successMessage: 'Nómina creada exitosamente.',
        reloadOnSuccess: false,
        onSuccess: (data) => {
            const id = data?.data?.id;
            window.location.href = id ? '/rrhh/nomina/' + id : '/rrhh/nomina';
        }
    });
}

// ── ver.html ──────────────────────────────────────────────────────────────────

/**
 * Executes a lifecycle action (calcular / aprobar / contabilizar) via POST.
 * On success redirects to the same detail page to reflect the new state.
 */
function accionNomina(id, accion, titulo, descripcion) {
    showConfirmDialog(titulo, descripcion, 'Confirmar').then(confirmed => {
        if (!confirmed) return;

        httpPost('/api/nomina/' + id + '/' + accion, {}, {
            successMessage: 'Operación realizada exitosamente.',
            reloadOnSuccess: false,
            onSuccess: () => {
                window.location.reload();
            }
        });
    });
}

/** Opens the anulación modal — stores the target id in a data attribute on the confirm button. */
function accionAnular(id) {
    const modal = document.getElementById('modalAnular');
    if (!modal) return;

    document.getElementById('motivoAnulacion').value = '';
    // Store id for use in confirmarAnular()
    modal.dataset.nominaId = id;

    const bsModal = bootstrap.Modal.getOrCreateInstance(modal);
    bsModal.show();
}

/** Reads motivo from the modal textarea and calls DELETE /api/nomina/{id}. */
function confirmarAnular() {
    const modal  = document.getElementById('modalAnular');
    const id     = modal?.dataset?.nominaId || nominaId;
    const motivo = document.getElementById('motivoAnulacion')?.value?.trim();

    if (!motivo) {
        showToast('error', 'Debe indicar un motivo de anulación.');
        return;
    }

    httpRequest({
        url: '/api/nomina/' + id,
        method: 'DELETE',
        data: { motivo },
        successMessage: 'Nómina anulada exitosamente.',
        reloadOnSuccess: false,
        onSuccess: () => {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) bsModal.hide();
            window.location.reload();
        }
    });
}
