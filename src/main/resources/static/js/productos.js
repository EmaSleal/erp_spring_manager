// ========================================
// Variables Globales
// ========================================
const rowsPerPage = 10;
let currentPage = 1;
let filtered = productos;
let productoModal;
let productoModalElement;

// ========================================
// Inicialización
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar modal de Bootstrap
    productoModalElement = document.getElementById('productoModal');
    if (productoModalElement) {
        productoModal = new bootstrap.Modal(productoModalElement);
    }
    
    // Configurar evento de guardado
    const btnGuardar = document.getElementById('btnGuardarProducto');
    if (btnGuardar) {
        btnGuardar.addEventListener('click', guardarProducto);
    }
    
    // Renderizar tabla inicial
    renderTable();
    
    // Re-renderizar paginación al cambiar tamaño de ventana
    let resizeTimer;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            renderPagination();
        }, 250); // Debounce de 250ms
    });
});

// ========================================
// Funciones de Modal
// ========================================
function openAddModal() {
    // Cambiar título del modal
    document.getElementById('modalTitle').textContent = 'Agregar Producto';
    
    // Limpiar formulario
    const form = document.getElementById('productoForm');
    form.reset();
    form.classList.remove('was-validated');
    
    // Limpiar ID oculto
    document.getElementById('idProducto').value = '';
    
    // Habilitar campos que podrían estar deshabilitados
    document.getElementById('codigo').disabled = false;
    document.getElementById('presentacion').disabled = false;
    
    // Marcar como activo por defecto
    document.getElementById('active').checked = true;
    
    // Abrir modal
    productoModal.show();
}

function openEditModal(idProducto, codigo, descripcion, presentacionId, precioInst, precioMay, activo) {
    // Cambiar título del modal
    document.getElementById('modalTitle').textContent = 'Editar Producto';
    
    // Limpiar validación previa
    const form = document.getElementById('productoForm');
    form.classList.remove('was-validated');
    
    // Llenar campos del formulario
    document.getElementById('idProducto').value = idProducto;
    document.getElementById('codigo').value = codigo;
    document.getElementById('descripcion').value = descripcion;
    document.getElementById('presentacion').value = presentacionId;
    document.getElementById('precioInstitucional').value = precioInst;
    document.getElementById('precioMayorista').value = precioMay;
    document.getElementById('active').checked = activo;
    
    // El código no se puede editar en modo edición
    document.getElementById('codigo').disabled = true;
    document.getElementById('presentacion').disabled = true;
    
    // Abrir modal
    productoModal.show();
}

function guardarProducto() {
    const form = document.getElementById('productoForm');
    
    // Validar formulario
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
    }
    
    // Obtener datos del formulario
    const idProducto = document.getElementById('idProducto').value;
    
    // IMPORTANTE: Habilitar campos deshabilitados temporalmente para que se envíen
    const codigoInput = document.getElementById('codigo');
    const presentacionInput = document.getElementById('presentacion');
    const codigoWasDisabled = codigoInput.disabled;
    const presentacionWasDisabled = presentacionInput.disabled;
    
    // Habilitar temporalmente los campos deshabilitados
    codigoInput.disabled = false;
    presentacionInput.disabled = false;
    
    // Crear FormData después de habilitar los campos
    const formData = new FormData(form);
    
    // Restaurar el estado disabled si era necesario
    codigoInput.disabled = codigoWasDisabled;
    presentacionInput.disabled = presentacionWasDisabled;
    
    // Determinar URL según si es crear o editar
    const url = idProducto ? '/productos/actualizar' : '/productos/guardar';
    
    // Enviar datos
    fetch(url, {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (response.ok) {
            // Cerrar modal
            productoModal.hide();
            
            // Mostrar mensaje de éxito
            Swal.fire({
                icon: 'success',
                title: '¡Éxito!',
                text: idProducto ? 'Producto actualizado correctamente' : 'Producto agregado correctamente',
                timer: 2000,
                showConfirmButton: false
            }).then(() => {
                // Recargar página
                window.location.reload();
            });
        } else {
            throw new Error('Error al guardar el producto');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar el producto. Intente nuevamente.',
            confirmButtonText: 'Aceptar'
        });
    });
}

function eliminarProducto(idProducto, descripcion) {
    Swal.fire({
        title: '¿Eliminar producto?',
        text: `¿Está seguro de eliminar "${descripcion}"? Esta acción no se puede deshacer.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            // Redirigir a la URL de eliminación
            window.location.href = `/productos/eliminar/${idProducto}`;
        }
    });
}

// ========================================
// Funciones de Tabla
// ========================================
function renderTable() {
    const start = (currentPage - 1) * rowsPerPage;
    const end = start + rowsPerPage;
    const paginatedItems = filtered.slice(start, end);

    const tbody = document.getElementById('productosBody');
    tbody.innerHTML = '';

    paginatedItems.forEach(producto => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="text-center">${producto.idProducto}</td>
            <td class="d-none d-md-table-cell"><span class="badge bg-secondary">${producto.codigo}</span></td>
            <td>
                <div class="fw-semibold">${producto.descripcion}</div>
                <small class="text-muted">${producto?.presentacion?.nombre}</small>
            </td>
            <td class="text-end">$${parseFloat(producto.precioInstitucional).toFixed(2)}</td>
            <td class="text-end d-none d-lg-table-cell">$${parseFloat(producto.precioMayorista).toFixed(2)}</td>
            <td class="text-center d-none d-md-table-cell">
                ${producto.active ? 
                    '<span class="badge bg-success"><i class="fas fa-check me-1"></i>Activo</span>' : 
                    '<span class="badge bg-danger"><i class="fas fa-times me-1"></i>Inactivo</span>'}
            </td>
            <td class="text-center">
                <button class="btn btn-sm btn-warning me-1" 
                        onclick="openEditModal(${producto.idProducto}, '${producto.codigo}', '${producto.descripcion}', ${producto?.presentacion?.idPresentacion}, ${producto.precioInstitucional}, ${producto.precioMayorista}, ${producto.active})"
                        title="Editar"
                        style="${(typeof userRole !== 'undefined' && (userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR')) ? 'display:none;' : ''}">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" 
                        onclick="eliminarProducto(${producto.idProducto}, '${producto.descripcion}')"
                        title="Eliminar"
                        style="${(typeof userRole !== 'undefined' && (userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR')) ? 'display:none;' : ''}">
                    <i class="fas fa-trash"></i>
                </button>
                ${(typeof userRole !== 'undefined' && (userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR')) ? 
                    '<span class="badge bg-secondary"><i class="bi bi-eye-fill"></i> Solo lectura</span>' : ''}
            </td>
        `;
        tbody.appendChild(row);
    });

    renderPagination();
}

function renderPagination() {
    const pageCount = Math.ceil(filtered.length / rowsPerPage);
    const pagination = document.getElementById('pagination');
    pagination.innerHTML = '';

    if (pageCount <= 1) return;

    // Calcular cuántas páginas mostrar según el ancho de pantalla
    const screenWidth = window.innerWidth;
    let maxVisiblePages;
    
    if (screenWidth < 576) {
        maxVisiblePages = 3; // Mobile: mostrar máximo 3 páginas
    } else if (screenWidth < 768) {
        maxVisiblePages = 5; // Mobile grande: 5 páginas
    } else if (screenWidth < 992) {
        maxVisiblePages = 7; // Tablet: 7 páginas
    } else {
        maxVisiblePages = 10; // Desktop: 10 páginas
    }

    // Botón anterior
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '<i class="fas fa-chevron-left"></i>';
    prevBtn.className = `btn btn-sm btn-outline-primary me-1 ${currentPage === 1 ? 'disabled' : ''}`;
    prevBtn.onclick = () => {
        if (currentPage > 1) {
            currentPage--;
            renderTable();
        }
    };
    pagination.appendChild(prevBtn);

    // Calcular qué páginas mostrar
    let startPage = 1;
    let endPage = pageCount;

    if (pageCount > maxVisiblePages) {
        // Calcular rango centrado en la página actual
        const halfVisible = Math.floor(maxVisiblePages / 2);
        startPage = Math.max(1, currentPage - halfVisible);
        endPage = Math.min(pageCount, startPage + maxVisiblePages - 1);
        
        // Ajustar si llegamos al final
        if (endPage === pageCount) {
            startPage = Math.max(1, pageCount - maxVisiblePages + 1);
        }
    }

    // Primera página siempre visible (si no está en el rango)
    if (startPage > 1) {
        const btn = document.createElement('button');
        btn.innerText = '1';
        btn.className = 'btn btn-sm btn-outline-primary me-1';
        btn.onclick = () => {
            currentPage = 1;
            renderTable();
        };
        pagination.appendChild(btn);

        // Separador "..."
        if (startPage > 2) {
            const dots = document.createElement('span');
            dots.innerText = '...';
            dots.className = 'mx-1';
            pagination.appendChild(dots);
        }
    }

    // Botones de páginas en el rango visible
    for (let i = startPage; i <= endPage; i++) {
        const btn = document.createElement('button');
        btn.innerText = i;
        btn.className = `btn btn-sm me-1 ${i === currentPage ? 'btn-primary' : 'btn-outline-primary'}`;
        btn.onclick = () => {
            currentPage = i;
            renderTable();
        };
        pagination.appendChild(btn);
    }

    // Última página siempre visible (si no está en el rango)
    if (endPage < pageCount) {
        // Separador "..."
        if (endPage < pageCount - 1) {
            const dots = document.createElement('span');
            dots.innerText = '...';
            dots.className = 'mx-1';
            pagination.appendChild(dots);
        }

        const btn = document.createElement('button');
        btn.innerText = pageCount;
        btn.className = 'btn btn-sm btn-outline-primary me-1';
        btn.onclick = () => {
            currentPage = pageCount;
            renderTable();
        };
        pagination.appendChild(btn);
    }

    // Botón siguiente
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '<i class="fas fa-chevron-right"></i>';
    nextBtn.className = `btn btn-sm btn-outline-primary ${currentPage === pageCount ? 'disabled' : ''}`;
    nextBtn.onclick = () => {
        if (currentPage < pageCount) {
            currentPage++;
            renderTable();
        }
    };
    pagination.appendChild(nextBtn);
}

function filtrarProductos() {
    const valor = document.getElementById('searchInput').value.toLowerCase();
    filtered = productos.filter(p => 
        p.descripcion.toLowerCase().includes(valor) ||
        p.codigo.toLowerCase().includes(valor) ||
        p.presentacion.nombre.toLowerCase().includes(valor)
    );
    currentPage = 1;
    renderTable();
}