## 🎨 COMPONENTES FRONTEND

### Vista: `/admin/usuarios/gestionar.html`

**Características:**
- Listado paginado de usuarios
- Filtros: nombre, rol, estado
- Búsqueda en tiempo real
- Acciones: Ver, Editar, Bloquear/Desbloquear, Eliminar
- Badges de estado (activo/bloqueado)
- Responsive con Bootstrap 5

```html
<!-- Panel de filtros -->
<div class="card mb-4">
    <div class="card-header">
        <h5><i class="bi bi-filter"></i> Filtros</h5>
    </div>
    <div class="card-body">
        <form method="get" th:action="@{/admin/usuarios/gestionar}">
            <div class="row">
                <div class="col-md-4">
                    <input type="text" 
                           name="busqueda" 
                           class="form-control" 
                           placeholder="Buscar por nombre o email"
                           th:value="${busqueda}">
                </div>
                <div class="col-md-3">
                    <select name="rol" class="form-select">
                        <option value="">Todos los roles</option>
                        <option th:each="r : ${T(api.whats_orders_manager.model.Rol).values()}"
                                th:value="${r}"
                                th:text="${r.descripcion}"
                                th:selected="${r == rol}"></option>
                    </select>
                </div>
                <div class="col-md-3">
                    <select name="activo" class="form-select">
                        <option value="">Todos los estados</option>
                        <option value="true" th:selected="${activo == true}">Activos</option>
                        <option value="false" th:selected="${activo == false}">Bloqueados</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary w-100">
                        <i class="bi bi-search"></i> Filtrar
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>

<!-- Tabla de usuarios -->
<div class="card">
    <div class="card-header d-flex justify-content-between">
        <h5><i class="bi bi-people"></i> Usuarios</h5>
        <a href="/admin/usuarios/crear" 
           class="btn btn-success"
           sec:authorize="hasAuthority('USUARIOS_CREAR')">
            <i class="bi bi-plus-circle"></i> Nuevo Usuario
        </a>
    </div>
    <div class="card-body">
        <table class="table table-hover">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Último Acceso</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="usuario : ${usuarios.content}">
                    <td th:text="${usuario.id}"></td>
                    <td th:text="${usuario.nombre}"></td>
                    <td th:text="${usuario.email}"></td>
                    <td>
                        <span class="badge bg-info" 
                              th:text="${usuario.rol.descripcion}"></span>
                    </td>
                    <td>
                        <span th:if="${usuario.activo && !usuario.bloqueado}"
                              class="badge bg-success">Activo</span>
                        <span th:if="${usuario.bloqueado}"
                              class="badge bg-danger">Bloqueado</span>
                        <span th:if="${!usuario.activo}"
                              class="badge bg-secondary">Inactivo</span>
                    </td>
                    <td th:text="${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}"></td>
                    <td>
                        <div class="btn-group">
                            <a th:href="@{/admin/usuarios/{id}/editar(id=${usuario.id})}"
                               class="btn btn-sm btn-primary"
                               sec:authorize="hasAuthority('USUARIOS_EDITAR')">
                                <i class="bi bi-pencil"></i>
                            </a>
                            
                            <button th:if="${!usuario.bloqueado}"
                                    type="button"
                                    class="btn btn-sm btn-warning"
                                    onclick="bloquearUsuario([[${usuario.id}]])"
                                    sec:authorize="hasAuthority('USUARIOS_BLOQUEAR')">
                                <i class="bi bi-lock"></i>
                            </button>
                            
                            <button th:if="${usuario.bloqueado}"
                                    type="button"
                                    class="btn btn-sm btn-success"
                                    onclick="desbloquearUsuario([[${usuario.id}]])"
                                    sec:authorize="hasAuthority('USUARIOS_DESBLOQUEAR')">
                                <i class="bi bi-unlock"></i>
                            </button>
                            
                            <button type="button"
                                    class="btn btn-sm btn-danger"
                                    onclick="eliminarUsuario([[${usuario.id}]])"
                                    sec:authorize="hasAuthority('USUARIOS_ELIMINAR')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- Paginación -->
        <nav th:if="${usuarios.totalPages > 1}">
            <ul class="pagination">
                <li class="page-item" th:classappend="${usuarios.first} ? 'disabled'">
                    <a class="page-link" 
                       th:href="@{/admin/usuarios/gestionar(page=${usuarios.number - 1})}">
                        Anterior
                    </a>
                </li>
                <li class="page-item" 
                    th:each="i : ${#numbers.sequence(0, usuarios.totalPages - 1)}"
                    th:classappend="${i == usuarios.number} ? 'active'">
                    <a class="page-link" 
                       th:href="@{/admin/usuarios/gestionar(page=${i})}"
                       th:text="${i + 1}"></a>
                </li>
                <li class="page-item" th:classappend="${usuarios.last} ? 'disabled'">
                    <a class="page-link" 
                       th:href="@{/admin/usuarios/gestionar(page=${usuarios.number + 1})}">
                        Siguiente
                    </a>
                </li>
            </ul>
        </nav>
    </div>
</div>

<script>
    function bloquearUsuario(id) {
        const motivo = prompt('Ingrese el motivo del bloqueo:');
        if (!motivo) return;

        fetch(`/admin/usuarios/${id}/bloquear`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'motivo=' + encodeURIComponent(motivo)
        })
        .then(response => {
            if (response.ok) {
                alert('Usuario bloqueado exitosamente');
                location.reload();
            } else {
                alert('Error al bloquear usuario');
            }
        });
    }

    function desbloquearUsuario(id) {
        if (!confirm('¿Desbloquear este usuario?')) return;

        fetch(`/admin/usuarios/${id}/desbloquear`, {
            method: 'POST'
        })
        .then(response => {
            if (response.ok) {
                alert('Usuario desbloqueado exitosamente');
                location.reload();
            } else {
                alert('Error al desbloquear usuario');
            }
        });
    }

    function eliminarUsuario(id) {
        if (!confirm('¿Está seguro de eliminar este usuario?')) return;

        fetch(`/admin/usuarios/${id}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (response.ok) {
                alert('Usuario eliminado exitosamente');
                location.reload();
            } else {
                alert('Error al eliminar usuario');
            }
        });
    }
</script>
```

---

