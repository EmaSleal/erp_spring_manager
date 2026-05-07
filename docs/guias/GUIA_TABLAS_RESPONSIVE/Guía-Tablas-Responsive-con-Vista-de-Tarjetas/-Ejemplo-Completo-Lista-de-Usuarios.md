## 📊 Ejemplo Completo: Lista de Usuarios

```html
<div class="responsive-table-container">
    
    <!-- DESKTOP: Tabla -->
    <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
                <tr>
                    <th>#</th>
                    <th>Usuario</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user, iter : ${usuarios}">
                    <td th:text="${iter.index + 1}">1</td>
                    <td>
                        <div class="d-flex align-items-center">
                            <div class="avatar-circle bg-primary me-2">
                                <span th:text="${#strings.substring(user.nombre, 0, 1)}">A</span>
                            </div>
                            <div>
                                <strong th:text="${user.nombre}">Nombre</strong><br>
                                <small th:text="${user.email}">email</small>
                            </div>
                        </div>
                    </td>
                    <td><span class="badge bg-info" th:text="${user.rol}">ROL</span></td>
                    <td>
                        <span class="badge" 
                              th:classappend="${user.activo ? 'bg-success' : 'bg-secondary'}"
                              th:text="${user.activo ? 'Activo' : 'Inactivo'}">Estado</span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-primary">Editar</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- MÓVIL: Tarjetas -->
    <div class="mobile-card-view">
        <div th:each="user : ${usuarios}" 
             class="mobile-card"
             th:classappend="${user.activo ? 'card-success' : 'card-secondary'}">
            
            <div class="mobile-card-header">
                <div class="mobile-card-avatar">
                    <div class="avatar-circle bg-primary">
                        <span th:text="${#strings.substring(user.nombre, 0, 1)}">A</span>
                    </div>
                    <div class="mobile-card-avatar-info">
                        <div class="mobile-card-avatar-name" th:text="${user.nombre}">Nombre</div>
                        <div class="mobile-card-avatar-subtitle" th:text="${user.email}">email</div>
                    </div>
                </div>
                <span class="mobile-card-status"
                      th:classappend="${user.activo ? 'status-active' : 'status-inactive'}">
                    <i class="bi" th:classappend="${user.activo ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}"></i>
                    <span th:text="${user.activo ? 'Activo' : 'Inactivo'}">Estado</span>
                </span>
            </div>

            <div class="mobile-card-body">
                <div class="mobile-card-row">
                    <span class="mobile-card-label">Rol</span>
                    <span class="mobile-card-value">
                        <span class="badge bg-info" th:text="${user.rol}">ROL</span>
                    </span>
                </div>
                <div class="mobile-card-row">
                    <span class="mobile-card-label">Teléfono</span>
                    <span class="mobile-card-value" th:text="${user.telefono}">999999999</span>
                </div>
            </div>

            <div class="mobile-card-footer mobile-card-actions">
                <button class="btn btn-sm btn-primary">
                    <i class="bi bi-pencil me-1"></i> Editar
                </button>
                <button class="btn btn-sm btn-danger">
                    <i class="bi bi-trash me-1"></i> Eliminar
                </button>
            </div>
        </div>
    </div>

</div>
```

