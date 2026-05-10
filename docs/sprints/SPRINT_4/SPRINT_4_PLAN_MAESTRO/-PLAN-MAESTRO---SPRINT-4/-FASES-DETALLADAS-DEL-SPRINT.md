## 📋 FASES DETALLADAS DEL SPRINT

### FASE 1: CONFIGURACIÓN DEL SISTEMA (CRÍTICA) ⭐⭐⭐
**Duración:** 3-4 días (24-32 horas)  
**Prioridad:** MÁXIMA  
**Estado:** 🔴 NO INICIADO

#### 1.1 Base de Datos - Configuración (4-6h)

**Migración:** `MIGRATION_CONFIGURACION_SPRINT_4.sql`

```sql
-- Tabla de configuración de empresa
CREATE TABLE configuracion_empresa (
    id_configuracion INT PRIMARY KEY AUTO_INCREMENT,
    razon_social VARCHAR(255) NOT NULL COMMENT 'Nombre legal de la empresa',
    nombre_comercial VARCHAR(255) COMMENT 'Nombre comercial',
    rfc VARCHAR(13) COMMENT 'Registro Federal de Contribuyentes',
    regimen_fiscal VARCHAR(100) COMMENT 'Régimen fiscal SAT',
    
    -- Dirección
    direccion_calle VARCHAR(255),
    direccion_numero VARCHAR(20),
    direccion_colonia VARCHAR(100),
    direccion_ciudad VARCHAR(100),
    direccion_estado VARCHAR(100),
    direccion_codigo_postal VARCHAR(10),
    direccion_pais VARCHAR(100) DEFAULT 'México',
    
    -- Contacto
    telefono VARCHAR(20),
    email VARCHAR(255),
    sitio_web VARCHAR(255),
    
    -- Logo y branding
    logo_url VARCHAR(500) COMMENT 'URL del logo de la empresa',
    color_primario VARCHAR(7) DEFAULT '#007bff' COMMENT 'Color hex principal',
    color_secundario VARCHAR(7) DEFAULT '#6c757d',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_rfc (rfc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Configuración general de la empresa';

-- Tabla de configuración de facturación
CREATE TABLE configuracion_facturacion (
    id_configuracion INT PRIMARY KEY AUTO_INCREMENT,
    
    -- Serie de documentos
    serie_factura VARCHAR(10) DEFAULT 'F' COMMENT 'Serie para facturas',
    folio_actual_factura INT DEFAULT 1 COMMENT 'Folio consecutivo actual',
    
    -- Configuración de IVA
    iva_default DECIMAL(5, 2) DEFAULT 16.00 COMMENT 'IVA por defecto (%)',
    aplicar_iva BOOLEAN DEFAULT TRUE COMMENT 'Aplicar IVA automáticamente',
    
    -- Moneda y divisa
    moneda VARCHAR(10) DEFAULT 'MXN' COMMENT 'Código de moneda ISO 4217',
    simbolo_moneda VARCHAR(5) DEFAULT '$' COMMENT 'Símbolo de moneda',
    decimales_moneda INT DEFAULT 2 COMMENT 'Decimales para montos',
    
    -- Términos de pago
    dias_credito_default INT DEFAULT 30 COMMENT 'Días de crédito por defecto',
    mensaje_factura TEXT COMMENT 'Mensaje/nota al pie de facturas',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Configuración de facturación';

-- Tabla de configuración de correo electrónico
CREATE TABLE configuracion_email (
    id_configuracion INT PRIMARY KEY AUTO_INCREMENT,
    
    -- Servidor SMTP
    smtp_host VARCHAR(255) NOT NULL COMMENT 'Servidor SMTP (ej. smtp.gmail.com)',
    smtp_port INT NOT NULL DEFAULT 587 COMMENT 'Puerto SMTP',
    smtp_usuario VARCHAR(255) NOT NULL COMMENT 'Usuario de correo',
    smtp_password VARCHAR(255) NOT NULL COMMENT 'Contraseña (encriptada)',
    
    -- Configuración
    smtp_ssl BOOLEAN DEFAULT FALSE COMMENT 'Usar SSL',
    smtp_tls BOOLEAN DEFAULT TRUE COMMENT 'Usar TLS',
    smtp_auth BOOLEAN DEFAULT TRUE COMMENT 'Requiere autenticación',
    
    -- Remitente
    email_remitente VARCHAR(255) NOT NULL COMMENT 'Email del remitente',
    nombre_remitente VARCHAR(255) COMMENT 'Nombre del remitente',
    
    -- Configuración de envío
    email_copia VARCHAR(255) COMMENT 'Email para copia (CC)',
    email_copia_oculta VARCHAR(255) COMMENT 'Email para copia oculta (BCC)',
    
    -- Estado
    activo BOOLEAN DEFAULT TRUE,
    ultimo_test TIMESTAMP COMMENT 'Última prueba de envío',
    estado_ultimo_test VARCHAR(50) COMMENT 'Resultado última prueba',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Configuración de correo electrónico';

-- Tabla de parámetros del sistema (clave-valor)
CREATE TABLE parametro_sistema (
    id_parametro INT PRIMARY KEY AUTO_INCREMENT,
    clave VARCHAR(100) UNIQUE NOT NULL COMMENT 'Clave única del parámetro',
    valor TEXT COMMENT 'Valor del parámetro',
    tipo_dato VARCHAR(20) DEFAULT 'STRING' COMMENT 'STRING, INTEGER, BOOLEAN, DECIMAL, DATE',
    categoria VARCHAR(50) COMMENT 'Categoría (GENERAL, FACTURACION, WHATSAPP, etc.)',
    descripcion VARCHAR(500) COMMENT 'Descripción del parámetro',
    editable BOOLEAN DEFAULT TRUE COMMENT 'Usuario puede editar',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_categoria (categoria),
    INDEX idx_clave (clave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Parámetros configurables del sistema';

-- Insertar configuración inicial
INSERT INTO configuracion_empresa (razon_social, nombre_comercial, telefono, email) 
VALUES ('Mi Empresa S.A. de C.V.', 'Mi Empresa', '555-1234', 'contacto@miempresa.com');

INSERT INTO configuracion_facturacion (serie_factura, folio_actual_factura, iva_default, moneda, simbolo_moneda) 
VALUES ('F', 1, 16.00, 'MXN', '$');

-- Parámetros iniciales del sistema
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion) VALUES
('sistema.nombre', 'ERP Orders Manager', 'STRING', 'GENERAL', 'Nombre del sistema'),
('sistema.version', '1.0.0', 'STRING', 'GENERAL', 'Versión del sistema'),
('sistema.mantenimiento', 'false', 'BOOLEAN', 'GENERAL', 'Modo mantenimiento activo'),
('factura.dias_antes_vencimiento_alerta', '3', 'INTEGER', 'FACTURACION', 'Días antes de vencer para alertar'),
('whatsapp.mensajes_automaticos', 'true', 'BOOLEAN', 'WHATSAPP', 'Envío automático de mensajes WhatsApp'),
('notificaciones.email_enabled', 'true', 'BOOLEAN', 'NOTIFICACIONES', 'Notificaciones por email habilitadas'),
('reportes.cache_minutos', '5', 'INTEGER', 'REPORTES', 'Minutos de caché para reportes');
```

#### 1.2 Backend - Modelos y Entidades (3-4h)

**Archivos a crear:**

```
src/main/java/api/astro/whats_orders_manager/
├── models/
│   ├── ConfiguracionEmpresa.java
│   ├── ConfiguracionFacturacion.java
│   ├── ConfiguracionEmail.java
│   └── ParametroSistema.java
├── models/dto/
│   ├── ConfiguracionEmpresaDTO.java
│   ├── ConfiguracionFacturacionDTO.java
│   ├── ConfiguracionEmailDTO.java
│   └── ParametroSistemaDTO.java
├── models/enums/
│   ├── TipoDatoParametro.java
│   └── CategoriaParametro.java
```

**Ejemplo - ConfiguracionEmpresa.java:**

```java
package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad que almacena la configuración general de la empresa.
 * Datos legales, fiscales, contacto y branding.
 */
@Entity
@Table(name = "configuracion_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer idConfiguracion;

    // Datos legales
    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(name = "nombre_comercial")
    private String nombreComercial;

    @Column(name = "rfc", length = 13)
    private String rfc;

    @Column(name = "regimen_fiscal", length = 100)
    private String regimenFiscal;

    // Dirección
    @Column(name = "direccion_calle")
    private String direccionCalle;

    @Column(name = "direccion_numero", length = 20)
    private String direccionNumero;

    @Column(name = "direccion_colonia", length = 100)
    private String direccionColonia;

    @Column(name = "direccion_ciudad", length = 100)
    private String direccionCiudad;

    @Column(name = "direccion_estado", length = 100)
    private String direccionEstado;

    @Column(name = "direccion_codigo_postal", length = 10)
    private String direccionCodigoPostal;

    @Column(name = "direccion_pais", length = 100)
    private String direccionPais = "México";

    // Contacto
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "sitio_web")
    private String sitioWeb;

    // Logo y branding
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "color_primario", length = 7)
    private String colorPrimario = "#007bff";

    @Column(name = "color_secundario", length = 7)
    private String colorSecundario = "#6c757d";

    // Auditoría
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    /**
     * Obtiene la dirección completa formateada
     */
    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();
        if (direccionCalle != null) sb.append(direccionCalle);
        if (direccionNumero != null) sb.append(" ").append(direccionNumero);
        if (direccionColonia != null) sb.append(", ").append(direccionColonia);
        if (direccionCiudad != null) sb.append(", ").append(direccionCiudad);
        if (direccionEstado != null) sb.append(", ").append(direccionEstado);
        if (direccionCodigoPostal != null) sb.append(", C.P. ").append(direccionCodigoPostal);
        if (direccionPais != null) sb.append(", ").append(direccionPais);
        return sb.toString();
    }

    /**
     * Obtiene el nombre para mostrar (comercial o razón social)
     */
    public String getNombreDisplay() {
        return nombreComercial != null && !nombreComercial.isEmpty() 
            ? nombreComercial 
            : razonSocial;
    }
}
```

#### 1.3 Backend - Repositories y Services (4-6h)

**Archivos a crear:**

```
src/main/java/api/astro/whats_orders_manager/
├── repositories/
│   ├── ConfiguracionEmpresaRepository.java
│   ├── ConfiguracionFacturacionRepository.java
│   ├── ConfiguracionEmailRepository.java
│   └── ParametroSistemaRepository.java
├── services/
│   ├── ConfiguracionEmpresaService.java
│   ├── ConfiguracionEmpresaServiceImpl.java
│   ├── ConfiguracionFacturacionService.java
│   ├── ConfiguracionFacturacionServiceImpl.java
│   ├── ConfiguracionEmailService.java
│   ├── ConfiguracionEmailServiceImpl.java
│   ├── ParametroSistemaService.java
│   └── ParametroSistemaServiceImpl.java
```

**Ejemplo - ParametroSistemaService.java:**

```java
package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.ParametroSistema;
import java.util.List;
import java.util.Optional;

public interface ParametroSistemaService {
    
    /**
     * Obtiene todos los parámetros del sistema
     */
    List<ParametroSistema> findAll();
    
    /**
     * Obtiene parámetros de una categoría específica
     */
    List<ParametroSistema> findByCategoria(String categoria);
    
    /**
     * Obtiene un parámetro por su clave
     */
    Optional<ParametroSistema> findByClave(String clave);
    
    /**
     * Obtiene el valor de un parámetro (String)
     */
    String getValor(String clave, String valorDefault);
    
    /**
     * Obtiene el valor de un parámetro como Integer
     */
    Integer getValorInteger(String clave, Integer valorDefault);
    
    /**
     * Obtiene el valor de un parámetro como Boolean
     */
    Boolean getValorBoolean(String clave, Boolean valorDefault);
    
    /**
     * Actualiza el valor de un parámetro
     */
    ParametroSistema actualizarValor(String clave, String nuevoValor);
    
    /**
     * Guarda o actualiza un parámetro
     */
    ParametroSistema save(ParametroSistema parametro);
    
    /**
     * Verifica si un parámetro existe
     */
    boolean existsByClave(String clave);
}
```

#### 1.4 Backend - Controllers (3-4h)

**Archivos a crear:**

```
src/main/java/api/astro/whats_orders_manager/controllers/
├── ConfiguracionController.java (Vista principal)
└── api/
    ├── ConfiguracionEmpresaRestController.java
    ├── ConfiguracionFacturacionRestController.java
    ├── ConfiguracionEmailRestController.java
    └── ParametroSistemaRestController.java
```

**Ejemplo - ConfiguracionController.java:**

```java
package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión de configuración del sistema.
 * Solo accesible para usuarios con rol ADMIN.
 */
@Controller
@RequestMapping("/configuracion")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class ConfiguracionController {

    @Autowired
    private ConfiguracionEmpresaService empresaService;

    @Autowired
    private ConfiguracionFacturacionService facturacionService;

    @Autowired
    private ConfiguracionEmailService emailService;

    @Autowired
    private ParametroSistemaService parametroService;

    /**
     * Muestra la página principal de configuración
     */
    @GetMapping
    public String verConfiguracion(
            @RequestParam(value = "tab", defaultValue = "empresa") String tab,
            Model model) {
        
        log.info("Accediendo a configuración - Tab: {}", tab);

        // Cargar datos según el tab activo
        switch (tab) {
            case "empresa":
                model.addAttribute("empresa", empresaService.getConfiguracion());
                break;
            case "facturacion":
                model.addAttribute("facturacion", facturacionService.getConfiguracion());
                break;
            case "email":
                model.addAttribute("email", emailService.getConfiguracion());
                break;
            case "parametros":
                model.addAttribute("parametros", parametroService.findAll());
                model.addAttribute("categorias", parametroService.getCategorias());
                break;
        }

        model.addAttribute("tabActivo", tab);
        return "configuracion/configuracion";
    }

    /**
     * Página de ayuda de configuración
     */
    @GetMapping("/ayuda")
    public String ayuda() {
        return "configuracion/ayuda";
    }
}
```

#### 1.5 Frontend - Vistas (6-8h)

**Archivos a crear:**

```
src/main/resources/templates/configuracion/
├── configuracion.html (Vista principal con tabs)
├── fragments/
│   ├── tab-empresa.html
│   ├── tab-facturacion.html
│   ├── tab-email.html
│   └── tab-parametros.html
└── ayuda.html
```

**Ejemplo - configuracion.html:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/layout}">
<head>
    <title>Configuración - ERP Orders Manager</title>
    <style>
        .config-tabs {
            border-bottom: 2px solid #dee2e6;
            margin-bottom: 2rem;
        }
        .config-tabs .nav-link {
            color: #495057;
            font-weight: 500;
            padding: 1rem 1.5rem;
            border: none;
            border-bottom: 3px solid transparent;
            transition: all 0.3s ease;
        }
        .config-tabs .nav-link:hover {
            border-bottom-color: #007bff;
            color: #007bff;
        }
        .config-tabs .nav-link.active {
            color: #007bff;
            border-bottom-color: #007bff;
            background-color: transparent;
        }
        .config-section {
            background: white;
            padding: 2rem;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .config-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2rem;
        }
        .form-section {
            margin-bottom: 2rem;
            padding-bottom: 2rem;
            border-bottom: 1px solid #e9ecef;
        }
        .form-section:last-child {
            border-bottom: none;
        }
        .section-title {
            font-size: 1.1rem;
            font-weight: 600;
            color: #495057;
            margin-bottom: 1rem;
            display: flex;
            align-items: center;
        }
        .section-title i {
            margin-right: 0.5rem;
            color: #007bff;
        }
    </style>
</head>
<body>
<div layout:fragment="content">
    <div class="container-fluid">
        <!-- Header -->
        <div class="config-header">
            <div>
                <h2>
                    <i class="fas fa-cog text-primary me-2"></i>
                    Configuración del Sistema
                </h2>
                <p class="text-muted mb-0">
                    Administra los parámetros generales del sistema
                </p>
            </div>
            <div>
                <a th:href="@{/configuracion/ayuda}" class="btn btn-outline-secondary" target="_blank">
                    <i class="fas fa-question-circle me-1"></i>
                    Ayuda
                </a>
            </div>
        </div>

        <!-- Tabs de navegación -->
        <ul class="nav nav-tabs config-tabs" role="tablist">
            <li class="nav-item">
                <a class="nav-link" 
                   th:classappend="${tabActivo == 'empresa'} ? 'active'"
                   th:href="@{/configuracion(tab='empresa')}">
                    <i class="fas fa-building me-2"></i>
                    Empresa
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link"
                   th:classappend="${tabActivo == 'facturacion'} ? 'active'"
                   th:href="@{/configuracion(tab='facturacion')}">
                    <i class="fas fa-file-invoice-dollar me-2"></i>
                    Facturación
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link"
                   th:classappend="${tabActivo == 'email'} ? 'active'"
                   th:href="@{/configuracion(tab='email')}">
                    <i class="fas fa-envelope me-2"></i>
                    Correo Electrónico
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link"
                   th:classappend="${tabActivo == 'parametros'} ? 'active'"
                   th:href="@{/configuracion(tab='parametros')}">
                    <i class="fas fa-sliders-h me-2"></i>
                    Parámetros
                </a>
            </li>
        </ul>

        <!-- Contenido de tabs -->
        <div class="tab-content">
            <!-- Tab Empresa -->
            <div th:if="${tabActivo == 'empresa'}" class="config-section">
                <div th:replace="~{configuracion/fragments/tab-empresa :: empresa-content}"></div>
            </div>

            <!-- Tab Facturación -->
            <div th:if="${tabActivo == 'facturacion'}" class="config-section">
                <div th:replace="~{configuracion/fragments/tab-facturacion :: facturacion-content}"></div>
            </div>

            <!-- Tab Email -->
            <div th:if="${tabActivo == 'email'}" class="config-section">
                <div th:replace="~{configuracion/fragments/tab-email :: email-content}"></div>
            </div>

            <!-- Tab Parámetros -->
            <div th:if="${tabActivo == 'parametros'}" class="config-section">
                <div th:replace="~{configuracion/fragments/tab-parametros :: parametros-content}"></div>
            </div>
        </div>
    </div>
</div>

<th:block layout:fragment="scripts">
    <script th:src="@{/js/configuracion.js}"></script>
</th:block>
</body>
</html>
```

#### 1.6 JavaScript y Validaciones (2-3h)

**Archivos a crear:**

```
src/main/resources/static/js/
├── configuracion.js (Lógica general)
├── configuracion-empresa.js
├── configuracion-facturacion.js
├── configuracion-email.js
└── configuracion-parametros.js
```

#### 1.7 Testing y Validación (3-4h)

- [ ] Tests unitarios de servicios
- [ ] Tests de integración de controllers
- [ ] Pruebas de validación de formularios
- [ ] Pruebas de envío de email de prueba
- [ ] Verificación de encriptación de passwords SMTP

**Entregables Fase 1:**
- ✅ 4 tablas de configuración creadas
- ✅ 4 entidades JPA + DTOs
- ✅ 4 repositories + 4 services
- ✅ 1 controller web + 4 REST controllers
- ✅ Vista de configuración con 4 tabs
- ✅ JavaScript con validaciones
- ✅ Tests unitarios y de integración

---

### FASE 2: REPORTES AVANZADOS ⭐⭐⭐
**Duración:** 4-5 días (32-40 horas)  
**Prioridad:** ALTA  
**Estado:** 🔴 NO INICIADO

[**Nota:** Esta fase será detallada en el siguiente documento]

---

### FASE 3: NOTIFICACIONES ⭐⭐
**Duración:** 3-4 días (24-32 horas)  
**Prioridad:** MEDIA-ALTA  
**Estado:** 🔴 NO INICIADO

[**Nota:** Esta fase será detallada en el siguiente documento]

---

### FASE 4: USUARIOS AVANZADO ⭐⭐
**Duración:** 2-3 días (16-24 horas)  
**Prioridad:** MEDIA  
**Estado:** 🔴 NO INICIADO

[**Nota:** Esta fase será detallada en el siguiente documento]

---

