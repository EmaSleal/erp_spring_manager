/**
 * ============================================================================
 * CONFIGURACION-PARAMETROS.JS - Gestión CRUD de parámetros del sistema
 * WhatsApp Orders Manager - Sprint 4
 * ============================================================================
 */

const ConfiguracionParametros = {
    API_URL: '/api/configuracion/parametros',
    parametros: [],
    parametrosFiltrados: [],
    
    /**
     * Inicializa el módulo
     */
    init: function() {
        this.cargarParametros();
        this.configurarEventos();
        console.log('✅ Módulo Parámetros inicializado');
    },
    
    /**
     * Configura event listeners
     */
    configurarEventos: function() {
        // Botón nuevo parámetro
        const btnNuevo = document.getElementById('btn-nuevo-parametro');
        if (btnNuevo) {
            btnNuevo.addEventListener('click', () => this.mostrarModalNuevo());
        }
        
        // Botón inicializar parámetros
        const btnInicializar = document.getElementById('btn-inicializar-parametros');
        if (btnInicializar) {
            btnInicializar.addEventListener('click', () => this.inicializarParametros());
        }
        
        // Form crear/editar parámetro
        const formParametro = document.getElementById('form-parametro');
        if (formParametro) {
            formParametro.addEventListener('submit', (e) => {
                e.preventDefault();
                this.guardarParametro();
            });
        }
        
        // Form edición rápida de valor
        const formEditarValor = document.getElementById('form-editar-valor');
        if (formEditarValor) {
            formEditarValor.addEventListener('submit', (e) => {
                e.preventDefault();
                this.actualizarValor();
            });
        }
        
        // Botón actualizar valor (del modal de edición)
        const btnActualizarValor = document.getElementById('btn-actualizar-valor');
        if (btnActualizarValor) {
            btnActualizarValor.addEventListener('click', () => this.actualizarValor());
        }
        
        // Búsqueda
        const inputBusqueda = document.getElementById('buscar-parametro');
        if (inputBusqueda) {
            inputBusqueda.addEventListener('input', (e) => {
                this.buscarParametros(e.target.value);
            });
        }
        
        // Filtro por categoría
        const selectCategoria = document.getElementById('filtro-categoria');
        if (selectCategoria) {
            selectCategoria.addEventListener('change', (e) => {
                this.filtrarPorCategoria(e.target.value);
            });
        }
        
        // Filtro por tipo de parámetro
        const radiosEditable = document.querySelectorAll('input[name="filtro-editable"]');
        radiosEditable.forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.filtrarPorEditable(e.target.value);
            });
        });
    },
    
    /**
     * Carga todos los parámetros
     */
    cargarParametros: async function() {
        try {
            const response = await Configuracion.get(this.API_URL);
            
            if (response.success && response.data) {
                this.parametros = response.data;
                this.parametrosFiltrados = [...this.parametros];
                this.renderizarTabla();
                this.actualizarEstadisticas();
                console.log(`✅ ${this.parametros.length} parámetros cargados`);
            } else {
                this.parametros = [];
                this.parametrosFiltrados = [];
                this.renderizarTabla();
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'info', 
                    'No hay parámetros configurados. Usa el botón "Inicializar Parámetros".');
            }
        } catch (error) {
            console.error('❌ Error cargando parámetros:', error);
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                'Error al cargar los parámetros');
        }
    },
    
    /**
     * Renderiza la tabla de parámetros
     */
    renderizarTabla: function() {
        const tbody = document.getElementById('tabla-parametros');
        if (!tbody) return;
        
        if (this.parametrosFiltrados.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-muted py-4">
                        <i class="fas fa-inbox fa-3x mb-3"></i>
                        <p>No hay parámetros para mostrar</p>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.parametrosFiltrados.map(param => `
            <tr>
                <td>
                    <strong>${param.clave}</strong>
                    ${param.descripcion ? `<br><small class="text-muted">${param.descripcion}</small>` : ''}
                </td>
                <td>
                    <code>${this.formatearValor(param.valor, param.tipoDato)}</code>
                </td>
                <td>
                    <span class="badge ${this.getBadgeTipo(param.tipoDato)}">
                        ${param.tipoDato}
                    </span>
                </td>
                <td>
                    <span class="badge bg-secondary">
                        ${param.categoria || 'General'}
                    </span>
                </td>
                <td class="text-center">
                    ${param.editable ? 
                        '<i class="fas fa-check text-success"></i>' : 
                        '<i class="fas fa-lock text-warning"></i>'}
                </td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" 
                                onclick="ConfiguracionParametros.mostrarModalEditar('${param.clave}')"
                                title="Editar valor">
                            <i class="fas fa-edit"></i>
                        </button>
                        ${param.editable ? `
                        <button class="btn btn-outline-danger" 
                                onclick="ConfiguracionParametros.eliminarParametro('${param.clave}')"
                                title="Eliminar">
                            <i class="fas fa-trash"></i>
                        </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `).join('');
    },
    
    /**
     * Formatea el valor según el tipo de dato
     * - Maneja booleanos representados como boolean, 'true'/'false', '1'/'0'
     * - Acepta tipos STRING y TEXT de forma segura
     */
    formatearValor: function(valor, tipo) {
        if (valor === null || valor === undefined) return '-';

        // Normalizar tipo (aceptar tanto STRING como TEXT si aplica)
        const t = (tipo || '').toUpperCase();

        try {
            switch (t) {
                case 'BOOLEAN': {
                    // valor puede ser booleano, string 'true'/'false' o '1'/'0'
                    if (typeof valor === 'string') {
                        const v = valor.trim().toLowerCase();
                        return (v === 'true' || v === '1') ? 'Sí' : 'No';
                    }
                    if (typeof valor === 'number') {
                        return valor === 1 ? 'Sí' : 'No';
                    }
                    return valor ? 'Sí' : 'No';
                }

                case 'INTEGER':
                case 'LONG': {
                    const n = Number(valor);
                    return Number.isNaN(n) ? String(valor) : String(Math.trunc(n));
                }

                case 'DECIMAL': {
                    const f = parseFloat(valor);
                    return Number.isNaN(f) ? String(valor) : ('' + f);
                }

                case 'DATE': {
                    // Intentar formatear ISO (YYYY-MM-DD) a formato legible
                    const s = String(valor);
                    // Si viene como YYYY-MM-DD, mostrar igual; si incluye hora, truncar
                    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
                    const m = s.match(/(\d{4}-\d{2}-\d{2})/);
                    return m ? m[1] : s;
                }

                case 'STRING':
                case 'TEXT':
                default: {
                    const s = String(valor);
                    return s.length > 50 ? s.substring(0, 47) + '...' : s;
                }
            }
        } catch (e) {
            // Fallback por seguridad
            return String(valor);
        }
    },
    
    /**
     * Obtiene la clase de badge según el tipo de dato
     */
    getBadgeTipo: function(tipo) {
        const badges = {
            'TEXT': 'bg-info',
            'STRING': 'bg-info',
            'INTEGER': 'bg-primary',
            'DECIMAL': 'bg-success',
            'BOOLEAN': 'bg-warning text-dark'
        };
        return badges[tipo] || 'bg-secondary';
    },
    
    /**
     * Actualiza las estadísticas
     */
    actualizarEstadisticas: function() {
        const total = this.parametros.length;
        const editables = this.parametros.filter(p => p.editable).length;
        const sistema = this.parametros.filter(p => !p.editable).length;
        
        // Actualizar estadísticas solo si los elementos existen
        const statTotal = document.getElementById('stat-total');
        const statEditables = document.getElementById('stat-editables');
        const statSistema = document.getElementById('stat-sistema');
        
        if (statTotal) statTotal.textContent = total;
        if (statEditables) statEditables.textContent = editables;
        if (statSistema) statSistema.textContent = sistema;
    },
    
    /**
     * Busca parámetros por texto
     */
    buscarParametros: function(query) {
        if (!query || query.trim() === '') {
            this.parametrosFiltrados = [...this.parametros];
        } else {
            const q = query.toLowerCase();
            this.parametrosFiltrados = this.parametros.filter(p => 
                p.clave.toLowerCase().includes(q) ||
                (p.descripcion && p.descripcion.toLowerCase().includes(q)) ||
                p.valor.toLowerCase().includes(q)
            );
        }
        this.renderizarTabla();
    },
    
    /**
     * Filtra por categoría
     */
    filtrarPorCategoria: async function(categoria) {
        if (!categoria || categoria === '') {
            this.cargarParametros();
            return;
        }
        
        try {
            const response = await Configuracion.get(`${this.API_URL}/categoria/${categoria}`);
            
            if (response.success && response.data) {
                this.parametrosFiltrados = response.data;
                this.renderizarTabla();
            }
        } catch (error) {
            console.error('❌ Error filtrando por categoría:', error);
        }
    },
    
    /**
     * Filtra por tipo (todos, editables, sistema)
     */
    filtrarPorEditable: function(filtro) {
        if (filtro === 'todos') {
            this.parametrosFiltrados = [...this.parametros];
        } else if (filtro === 'editables') {
            this.parametrosFiltrados = this.parametros.filter(p => p.editable);
        } else if (filtro === 'sistema') {
            this.parametrosFiltrados = this.parametros.filter(p => !p.editable);
        }
        this.renderizarTabla();
    },
    
    /**
     * Inicializa los 17 parámetros por defecto
     */
    inicializarParametros: async function() {
        const confirmar = await Configuracion.confirmar(
            '¿Inicializar Parámetros?',
            'Esto creará los 17 parámetros predeterminados del sistema. Los parámetros existentes no se modificarán.',
            'Sí, Inicializar'
        );
        
        if (!confirmar) return;
        
        try {
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'info', 
                '<i class="fas fa-spinner fa-spin"></i> Inicializando parámetros...');
            
            const response = await Configuracion.post(`${this.API_URL}/inicializar`, {});
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'success', 
                    response.message || 'Parámetros inicializados correctamente');
                this.cargarParametros();
            } else {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                    response.message || 'Error al inicializar parámetros');
            }
        } catch (error) {
            console.error('❌ Error inicializando parámetros:', error);
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                'Error al inicializar parámetros');
        }
    },
    
    /**
     * Muestra modal para nuevo parámetro
     */
    mostrarModalNuevo: function() {
        // Limpiar formulario
        document.getElementById('form-parametro').reset();
        document.getElementById('parametro-clave').readOnly = false;
        document.getElementById('parametro-editable').checked = true;
        
        // Mostrar modal
        const modal = new bootstrap.Modal(document.getElementById('modal-parametro'));
        modal.show();
    },
    
    /**
     * Muestra modal para editar valor
     */
    mostrarModalEditar: function(clave) {
        const parametro = this.parametros.find(p => p.clave === clave);
        if (!parametro) return;
        
        // Cargar datos en el modal de edición rápida (usando IDs correctos del HTML)
        document.getElementById('edit-clave').value = parametro.clave;
        document.getElementById('edit-clave-label').textContent = parametro.clave;
        document.getElementById('edit-tipo-label').textContent = parametro.tipoDato;
        
        // Crear el input apropiado según el tipo de dato
        const container = document.getElementById('edit-valor-container');
        let inputHTML = '';
        
        switch(parametro.tipoDato) {
            case 'BOOLEAN':
                const isChecked = parametro.valor === 'true';
                inputHTML = `
                    <div class="form-check form-switch">
                        <input class="form-check-input" 
                               type="checkbox" 
                               id="edit-valor" 
                               ${isChecked ? 'checked' : ''}>
                        <label class="form-check-label" for="edit-valor">
                            ${isChecked ? 'Activado' : 'Desactivado'}
                        </label>
                    </div>
                `;
                break;
            case 'INTEGER':
            case 'LONG':
                inputHTML = `
                    <input type="number" 
                           class="form-control" 
                           id="edit-valor" 
                           value="${parametro.valor}" 
                           step="1" 
                           required>
                `;
                break;
            case 'DECIMAL':
                inputHTML = `
                    <input type="number" 
                           class="form-control" 
                           id="edit-valor" 
                           value="${parametro.valor}" 
                           step="0.01" 
                           required>
                `;
                break;
            case 'DATE':
                inputHTML = `
                    <input type="date" 
                           class="form-control" 
                           id="edit-valor" 
                           value="${parametro.valor}" 
                           required>
                `;
                break;
            case 'STRING':
            default:
                // Para STRING o texto largo, usar textarea
                inputHTML = `
                    <textarea class="form-control" 
                              id="edit-valor" 
                              rows="3" 
                              required>${parametro.valor}</textarea>
                `;
                break;
        }
        
        container.innerHTML = inputHTML;
        
        // Si es checkbox, agregar listener para cambiar el texto del label
        if (parametro.tipoDato === 'BOOLEAN') {
            const checkbox = document.getElementById('edit-valor');
            const label = checkbox.nextElementSibling;
            checkbox.addEventListener('change', function() {
                label.textContent = this.checked ? 'Activado' : 'Desactivado';
            });
        }
        
        // Mostrar modal
        const modal = new bootstrap.Modal(document.getElementById('modal-editar-valor'));
        modal.show();
    },
    
    /**
     * Guarda un nuevo parámetro
     */
    guardarParametro: async function() {
        if (!Configuracion.validarFormulario('form-parametro')) {
            return;
        }
        
        try {
            const datos = {
                clave: document.getElementById('parametro-clave').value,
                valor: document.getElementById('parametro-valor').value,
                tipoDato: document.getElementById('parametro-tipo').value,
                descripcion: document.getElementById('parametro-descripcion').value || null,
                categoria: document.getElementById('parametro-categoria').value || null,
                editable: document.getElementById('parametro-editable').checked
            };
            
            const response = await Configuracion.post(this.API_URL, datos);
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'success', 
                    'Parámetro creado exitosamente');
                
                // Cerrar modal
                bootstrap.Modal.getInstance(document.getElementById('modal-parametro')).hide();
                
                // Recargar
                this.cargarParametros();
            } else {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                    response.message || 'Error al crear el parámetro');
            }
        } catch (error) {
            console.error('❌ Error guardando parámetro:', error);
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                'Error al guardar el parámetro');
        }
    },
    
    /**
     * Actualiza el valor de un parámetro existente
     */
    actualizarValor: async function() {
        const clave = document.getElementById('edit-clave').value;
        const inputValor = document.getElementById('edit-valor');
        
        if (!inputValor) {
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'warning', 
                'Error al obtener el valor');
            return;
        }
        
        // Obtener el valor según el tipo de input
        let nuevoValor;
        if (inputValor.type === 'checkbox') {
            nuevoValor = inputValor.checked ? 'true' : 'false';
        } else {
            nuevoValor = inputValor.value.trim();
            if (!nuevoValor) {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'warning', 
                    'El valor no puede estar vacío');
                return;
            }
        }
        
        try {
            const response = await Configuracion.patch(`${this.API_URL}/${clave}`, {
                valor: nuevoValor
            });
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'success', 
                    'Valor actualizado exitosamente');
                
                // Cerrar modal
                const modalElement = document.getElementById('modal-editar-valor');
                const modalInstance = bootstrap.Modal.getInstance(modalElement);
                if (modalInstance) {
                    modalInstance.hide();
                }
                
                // Recargar parámetros
                setTimeout(() => this.cargarParametros(), 500);
            } else {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                    response.message || 'Error al actualizar el valor');
            }
        } catch (error) {
            console.error('❌ Error actualizando valor:', error);
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                'Error al actualizar el valor');
        }
    },
    
    /**
     * Elimina un parámetro
     */
    eliminarParametro: async function(clave) {
        const confirmar = await Configuracion.confirmar(
            '¿Eliminar Parámetro?',
            `¿Estás seguro de eliminar el parámetro "${clave}"? Esta acción no se puede deshacer.`,
            'Sí, Eliminar'
        );
        
        if (!confirmar) return;
        
        try {
            const response = await Configuracion.delete(`${this.API_URL}/${clave}`);
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'success', 
                    'Parámetro eliminado exitosamente');
                this.cargarParametros();
            } else {
                Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                    response.message || 'Error al eliminar el parámetro');
            }
        } catch (error) {
            console.error('❌ Error eliminando parámetro:', error);
            Configuracion.mostrarAlertaEn('alert-parametros-container', 'danger', 
                'Error al eliminar el parámetro');
        }
    }
};

// Inicializar cuando el tab de parámetros esté activo
document.addEventListener('shown.bs.tab', function (event) {
    if (event.target.id === 'parametros-tab') {
        ConfiguracionParametros.init();
    }
});

// Inicializar si ya está en el tab
document.addEventListener('DOMContentLoaded', function() {
    const parametrosTab = document.getElementById('parametros-tab');
    if (parametrosTab && parametrosTab.classList.contains('active')) {
        ConfiguracionParametros.init();
    }
});
