// ============================================================
// CARGA DINÁMICA DE UBICACIONES - SHARED
// Soporta:
// - Configuración Empresa (ids: provincia, canton, distrito)
// - Modal Cliente (ids: provinciaCliente, cantonCliente, distritoCliente)
// ============================================================

(function () {
    'use strict';

    let listenersEmpresaRegistrados = false;
    let listenersClienteRegistrados = false;

    function setOptions(select, items, emptyLabel) {
        if (!select) return;
        select.innerHTML = `<option value="">${emptyLabel}</option>`;
        items.forEach(item => {
            const option = document.createElement('option');
            option.value = item.codigo;
            option.textContent = `${item.codigo} - ${item.nombre}`;
            select.appendChild(option);
        });
    }

    async function fetchCantones(provinciaCodigo) {
        const response = await fetch(`/api/ubicaciones/cantones?provincia=${encodeURIComponent(provinciaCodigo)}`);
        if (!response.ok) throw new Error('Error al cargar cantones');
        return response.json();
    }

    async function fetchDistritos(provinciaCodigo, cantonCodigo) {
        const response = await fetch(`/api/ubicaciones/distritos?provincia=${encodeURIComponent(provinciaCodigo)}&canton=${encodeURIComponent(cantonCodigo)}`);
        if (!response.ok) throw new Error('Error al cargar distritos');
        return response.json();
    }

    async function cargarCantonesEn(provinciaCodigo, cantonSelectId, distritoSelectId, emptyLabel = '-- Seleccione --', cantonSeleccionado = '', distritoSeleccionado = '') {
        const cantonSelect = document.getElementById(cantonSelectId);
        const distritoSelect = document.getElementById(distritoSelectId);

        if (!cantonSelect || !distritoSelect) return;

        cantonSelect.innerHTML = '<option value="">-- Cargando... --</option>';
        cantonSelect.disabled = true;
        distritoSelect.innerHTML = '<option value="">-- Seleccione cantón primero --</option>';
        distritoSelect.disabled = true;

        if (!provinciaCodigo) {
            cantonSelect.innerHTML = '<option value="">-- Seleccione provincia primero --</option>';
            return;
        }

        try {
            const cantones = await fetchCantones(provinciaCodigo);

            setOptions(cantonSelect, cantones, emptyLabel);

            if (cantonSeleccionado) {
                cantonSelect.value = cantonSeleccionado;
            }

            cantonSelect.disabled = false;

            if (cantonSelect.value) {
                await cargarDistritosEn(provinciaCodigo, cantonSelect.value, distritoSelectId, emptyLabel, distritoSeleccionado);
            }
        } catch (error) {
            console.error('Error cargando cantones:', error);
            cantonSelect.innerHTML = '<option value="">Error al cargar</option>';
        }
    }

    async function cargarDistritosEn(provinciaCodigo, cantonCodigo, distritoSelectId, emptyLabel = '-- Seleccione --', distritoSeleccionado = '') {
        const distritoSelect = document.getElementById(distritoSelectId);
        if (!distritoSelect) return;

        distritoSelect.innerHTML = '<option value="">-- Cargando... --</option>';
        distritoSelect.disabled = true;

        if (!cantonCodigo) {
            distritoSelect.innerHTML = '<option value="">-- Seleccione cantón primero --</option>';
            return;
        }

        try {
            const distritos = await fetchDistritos(provinciaCodigo, cantonCodigo);
            setOptions(distritoSelect, distritos, emptyLabel);

            if (distritoSeleccionado) {
                distritoSelect.value = distritoSeleccionado;
            }

            distritoSelect.disabled = false;
        } catch (error) {
            console.error('Error cargando distritos:', error);
            distritoSelect.innerHTML = '<option value="">Error al cargar</option>';
        }
    }

    async function cargarProvinciasCliente() {
        const provinciaSelect = document.getElementById('provinciaCliente');
        const modal = document.getElementById('clienteModal');
        if (!provinciaSelect) return;

        const valorActual = modal?.dataset.provinciaCliente || provinciaSelect.value || '';
        const cantonSeleccionado = modal?.dataset.cantonCliente || '';
        const distritoSeleccionado = modal?.dataset.distritoCliente || '';

        try {
            const response = await fetch('/api/ubicaciones/provincias');
            if (!response.ok) throw new Error('Error al cargar provincias');
            const provincias = await response.json();

            setOptions(provinciaSelect, provincias, '-- Opcional --');

            if (valorActual) {
                provinciaSelect.value = valorActual;
                await cargarCantonesEn(valorActual, 'cantonCliente', 'distritoCliente', '-- Opcional --', cantonSeleccionado, distritoSeleccionado);
            }

            if (modal) {
                delete modal.dataset.provinciaCliente;
                delete modal.dataset.cantonCliente;
                delete modal.dataset.distritoCliente;
            }
        } catch (error) {
            console.error('Error cargando provincias (cliente):', error);
        }
    }

    function initUbicacionEmpresa() {
        const provinciaSelect = document.getElementById('provincia');
        const cantonSelect = document.getElementById('canton');

        if (!provinciaSelect || !cantonSelect) return;
        if (listenersEmpresaRegistrados) return;
        listenersEmpresaRegistrados = true;

        provinciaSelect.addEventListener('change', function () {
            const provinciaCodigo = this.value;
            if (provinciaCodigo) {
                cargarCantonesEn(provinciaCodigo, 'canton', 'distrito', '-- Seleccione --');
            } else {
                const distritoSelect = document.getElementById('distrito');
                cantonSelect.innerHTML = '<option value="">-- Seleccione provincia primero --</option>';
                cantonSelect.disabled = true;
                if (distritoSelect) {
                    distritoSelect.innerHTML = '<option value="">-- Seleccione cantón primero --</option>';
                    distritoSelect.disabled = true;
                }
            }
        });

        cantonSelect.addEventListener('change', function () {
            const provinciaCodigo = provinciaSelect.value;
            const cantonCodigo = this.value;
            if (cantonCodigo) {
                cargarDistritosEn(provinciaCodigo, cantonCodigo, 'distrito', '-- Seleccione --');
            } else {
                const distritoSelect = document.getElementById('distrito');
                if (distritoSelect) {
                    distritoSelect.innerHTML = '<option value="">-- Seleccione cantón primero --</option>';
                    distritoSelect.disabled = true;
                }
            }
        });

        if (provinciaSelect.value) {
            cargarCantonesEn(provinciaSelect.value, 'canton', 'distrito', '-- Seleccione --');
        }
    }

    function initUbicacionCliente() {
        const modal = document.getElementById('clienteModal');
        const provinciaSelect = document.getElementById('provinciaCliente');
        const cantonSelect = document.getElementById('cantonCliente');

        if (!provinciaSelect || !cantonSelect) return;

        if (!listenersClienteRegistrados) {
            listenersClienteRegistrados = true;

            provinciaSelect.addEventListener('change', function () {
                cargarCantonesEn(this.value, 'cantonCliente', 'distritoCliente', '-- Opcional --');
            });

            cantonSelect.addEventListener('change', function () {
                const provinciaCodigo = provinciaSelect.value;
                cargarDistritosEn(provinciaCodigo, this.value, 'distritoCliente', '-- Opcional --');
            });
        }

        if (modal) {
            modal.addEventListener('shown.bs.modal', function () {
                cargarProvinciasCliente();
            });
        } else {
            cargarProvinciasCliente();
        }
    }

    function initAll() {
        initUbicacionEmpresa();
        initUbicacionCliente();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }

    document.addEventListener('shown.bs.tab', function (event) {
        if (event.target && event.target.id === 'empresa-tab') {
            initUbicacionEmpresa();
        }
    });
})();
