## 🎨 COMPONENTES FRONTEND

### Vista: `editar.html`

**Ubicación:** `src/main/resources/templates/admin/empresa/editar.html`

**Estructura:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>Configuración de Empresa</title>
</head>
<body>
    <div layout:fragment="content">
        
        <!-- Breadcrumbs -->
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item">
                    <a href="/admin/dashboard">Inicio</a>
                </li>
                <li class="breadcrumb-item active">Configuración de Empresa</li>
            </ol>
        </nav>

        <!-- Mensajes flash -->
        <div th:if="${success}" class="alert alert-success">
            <i class="bi bi-check-circle"></i>
            <span th:text="${success}"></span>
        </div>

        <!-- Formulario principal -->
        <div class="card">
            <div class="card-header">
                <h4><i class="bi bi-building"></i> Datos de Empresa</h4>
            </div>
            <div class="card-body">
                <form th:action="@{/admin/empresa/actualizar}" 
                      method="post" 
                      th:object="${empresa}">
                    
                    <!-- Datos básicos -->
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Nombre de la empresa</label>
                                <input type="text" 
                                       class="form-control" 
                                       th:field="*{nombre}" 
                                       required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>CIF/NIF</label>
                                <input type="text" 
                                       class="form-control" 
                                       th:field="*{cif}">
                            </div>
                        </div>
                    </div>

                    <!-- Dirección -->
                    <div class="mb-3">
                        <label>Dirección</label>
                        <input type="text" class="form-control" th:field="*{direccion}">
                    </div>

                    <!-- Contacto -->
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Teléfono</label>
                                <input type="tel" class="form-control" th:field="*{telefono}">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Email</label>
                                <input type="email" class="form-control" th:field="*{email}">
                            </div>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-save"></i> Guardar cambios
                    </button>
                </form>
            </div>
        </div>

        <!-- Configuración SMTP -->
        <div class="card mt-4">
            <div class="card-header">
                <h4><i class="bi bi-envelope-at"></i> Configuración de Email (SMTP)</h4>
            </div>
            <div class="card-body">
                <form th:action="@{/admin/empresa/actualizar}" method="post">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Host SMTP</label>
                                <input type="text" 
                                       class="form-control" 
                                       th:field="*{smtpHost}"
                                       placeholder="smtp.gmail.com">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Puerto</label>
                                <input type="number" 
                                       class="form-control" 
                                       th:field="*{smtpPort}"
                                       placeholder="587">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Usuario SMTP</label>
                                <input type="text" class="form-control" th:field="*{smtpUsuario}">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Contraseña</label>
                                <input type="password" class="form-control" th:field="*{smtpPassword}">
                            </div>
                        </div>
                    </div>

                    <div class="form-check">
                        <input type="checkbox" 
                               class="form-check-input" 
                               th:field="*{smtpSsl}"
                               id="smtpSsl">
                        <label class="form-check-label" for="smtpSsl">
                            Usar SSL/TLS
                        </label>
                    </div>

                    <button type="submit" class="btn btn-primary mt-3">
                        <i class="bi bi-save"></i> Guardar configuración
                    </button>
                    
                    <button type="button" 
                            class="btn btn-outline-secondary mt-3"
                            onclick="enviarEmailPrueba()">
                        <i class="bi bi-send"></i> Enviar email de prueba
                    </button>
                </form>
            </div>
        </div>

        <!-- Logotipo -->
        <div class="card mt-4">
            <div class="card-header">
                <h4><i class="bi bi-image"></i> Logotipo de Empresa</h4>
            </div>
            <div class="card-body">
                <div th:if="${empresa.logoUrl}" class="mb-3">
                    <img th:src="${empresa.logoUrl}" 
                         alt="Logo actual" 
                         class="img-thumbnail"
                         style="max-height: 150px;">
                </div>

                <form th:action="@{/admin/empresa/logo}" 
                      method="post" 
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label>Seleccionar nuevo logotipo</label>
                        <input type="file" 
                               class="form-control" 
                               name="file" 
                               accept="image/*">
                    </div>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-upload"></i> Subir logotipo
                    </button>
                </form>
            </div>
        </div>

    </div>

    <!-- JavaScript -->
    <th:block layout:fragment="scripts">
        <script>
            function enviarEmailPrueba() {
                const destinatario = prompt('Ingrese el email de destino:');
                if (!destinatario) return;

                fetch('/admin/empresa/enviar-prueba', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'destinatario=' + encodeURIComponent(destinatario)
                })
                .then(response => response.json())
                .then(data => {
                    if (data.mensaje) {
                        alert('✅ ' + data.mensaje);
                    } else {
                        alert('❌ Error: ' + data.error);
                    }
                })
                .catch(error => {
                    alert('❌ Error de conexión: ' + error);
                });
            }
        </script>
    </th:block>
</body>
</html>
```

---

