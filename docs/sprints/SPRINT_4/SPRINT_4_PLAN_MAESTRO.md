# 🚀 PLAN MAESTRO - SPRINT 4

**Proyecto:** WhatsApp Orders Manager - Sistema ERP  
**Sprint:** 4  
**Objetivo:** Módulos de Gestión Avanzada (Reportes + Configuración + Usuarios + Notificaciones)  
**Duración:** 30 días / 15 días laborables  
**Estado:** 📝 PLANIFICADO

---

## 📊 ANÁLISIS DE SITUACIÓN ACTUAL

### ✅ Estado Actual del Proyecto

**Completado (100%):**
- ✅ Sprint 1: Autenticación, Dashboard, CRUDs básicos
- ✅ Sprint 2: WhatsApp mensajes básicos y plantillas
- ✅ Sprint 3 - Fase 1: Conversaciones WhatsApp + Reorganización de proyecto

**Total de archivos:** 99 archivos Java compilados  
**Última compilación:** BUILD SUCCESS (6.5 segundos)  
**Aplicación:** Funcional, 5.662 segundos de inicio  
**Base de datos:** MySQL 8.0 con HikariPool-1

### 📋 Módulos Pendientes Identificados

Según análisis de documentación existente (`SPRINT_1_RESUMEN_COMPLETO.md` línea 867):

1. **Reportes** - Gráficas avanzadas, exportación (PDF, Excel, CSV)
2. **Configuración** - Datos empresa, parámetros del sistema
3. **Usuarios** - CRUD avanzado con gestión de permisos
4. **Notificaciones** - Sistema de alertas (WhatsApp + Email)

**Módulos adicionales deseables:**
5. **Sistema Multi-Divisa** (Alta prioridad según MEJORAS_FUTURAS.md)
6. **WhatsApp Fase 2** - Envío activo de mensajes

---

## 🎯 OBJETIVOS DEL SPRINT 4

### Objetivo Principal
Completar los módulos de gestión avanzada que transformarán el sistema de un ERP básico a una plataforma completa de gestión empresarial con capacidades de análisis, configuración flexible y notificaciones automatizadas.

### Objetivos Específicos

1. **📊 Reportes Avanzados:**
   - Gráficas interactivas con Chart.js
   - Reportes financieros (ventas, ingresos, clientes)
   - Exportación (PDF, Excel, CSV)
   - Filtros avanzados (fechas, clientes, productos)

2. **⚙️ Configuración del Sistema:**
   - Datos de la empresa (razón social, RFC, logo)
   - Parámetros de facturación (IVA, moneda, series)
   - Configuración de correo electrónico (SMTP)
   - Parámetros generales del sistema

3. **👥 Gestión de Usuarios Avanzada:**
   - CRUD completo de usuarios (admin)
   - Gestión de roles y permisos granulares
   - Histórico de actividad de usuarios
   - Bloqueo/desbloqueo de cuentas

4. **🔔 Sistema de Notificaciones:**
   - Notificaciones en tiempo real (web)
   - Alertas por correo electrónico
   - Integración con WhatsApp (mensajes automáticos)
   - Configuración de preferencias de notificaciones

---

## 📊 PRIORIZACIÓN DE MÓDULOS

### Criterios de Priorización

| Criterio | Peso | Reportes | Configuración | Usuarios | Notificaciones |
|----------|------|----------|---------------|----------|----------------|
| **Valor de Negocio** | 30% | 9/10 | 8/10 | 7/10 | 8/10 |
| **Dependencias** | 25% | 5/10 | 10/10 | 8/10 | 6/10 |
| **Complejidad** | 20% | 7/10 | 4/10 | 6/10 | 8/10 |
| **Urgencia** | 15% | 8/10 | 9/10 | 6/10 | 7/10 |
| **Impacto UX** | 10% | 9/10 | 6/10 | 5/10 | 8/10 |
| **Total Ponderado** | 100% | **7.45** | **7.85** | **6.65** | **7.30** |

### 🏆 Orden de Implementación Recomendado

```
1. CONFIGURACIÓN (7.85) ⭐⭐⭐ CRÍTICO
   └─ Fundamento de todo el sistema
   └─ Otros módulos dependen de sus parámetros
   └─ 3-4 días de desarrollo

2. REPORTES (7.45) ⭐⭐⭐ ALTA PRIORIDAD
   └─ Alto valor de negocio
   └─ Análisis de datos crítico
   └─ 4-5 días de desarrollo

3. NOTIFICACIONES (7.30) ⭐⭐ MEDIA-ALTA
   └─ Mejora experiencia usuario
   └─ Automatización de comunicaciones
   └─ 3-4 días de desarrollo

4. USUARIOS (6.65) ⭐⭐ MEDIA
   └─ Importante pero no bloqueante
   └─ Sistema básico ya funciona
   └─ 2-3 días de desarrollo
```

**Justificación del Orden:**

1. **Configuración primero** porque otros módulos dependen de:
   - Datos de empresa para reportes
   - Configuración SMTP para notificaciones email
   - Parámetros de facturación para reportes financieros

2. **Reportes segundo** porque:
   - Alto valor de negocio (análisis de ventas)
   - Usa configuración de empresa
   - Independiente de notificaciones y usuarios

3. **Notificaciones tercero** porque:
   - Requiere configuración SMTP
   - Puede integrarse con reportes (enviar reportes por email)

4. **Usuarios al final** porque:
   - El sistema básico ya funciona
   - No bloquea otros módulos
   - Puede refinarse posteriormente

---

## 💰 ANÁLISIS COSTO-BENEFICIO

### Beneficios Esperados por Módulo

#### 📊 Reportes Avanzados
**Beneficios tangibles:**
- ⏱️ Ahorro de tiempo: 5-10 horas/mes en análisis manual
- 💰 Mejor toma de decisiones: +15% en efectividad de ventas
- 📈 Identificación de tendencias: +20% de oportunidades detectadas

**Beneficios intangibles:**
- Profesionalismo ante clientes
- Transparencia en gestión
- Auditoría facilitada

**ROI estimado:** $500-800 USD/mes en ahorro de tiempo + mejor toma decisiones

#### ⚙️ Configuración
**Beneficios tangibles:**
- ⏱️ Reducción de hardcoding: 100% parámetros configurables
- 🔧 Flexibilidad: Cambios sin código (0 horas desarrollo por cambio)
- 🏢 Multi-empresa: Soporte para múltiples negocios

**Beneficios intangibles:**
- Sistema más profesional
- Escalabilidad mejorada
- Mantenimiento simplificado

**ROI estimado:** $200-400 USD/mes en reducción de mantenimiento

#### 👥 Usuarios Avanzado
**Beneficios tangibles:**
- 🔒 Seguridad mejorada: -80% riesgo de accesos no autorizados
- 📊 Auditoría completa: 100% trazabilidad de acciones
- 👨‍💼 Gestión eficiente: 3-5 min/usuario vs 15-20 min manual

**Beneficios intangibles:**
- Cumplimiento de normativas
- Confianza de usuarios
- Control granular

**ROI estimado:** $100-200 USD/mes en seguridad y auditoría

#### 🔔 Notificaciones
**Beneficios tangibles:**
- ⏱️ Reducción de trabajo manual: 10-15 horas/mes
- 📞 Menos llamadas/emails manuales: -70% comunicaciones manuales
- 💰 Cobros más rápidos: +25% facturas pagadas a tiempo

**Beneficios intangibles:**
- Satisfacción del cliente mejorada
- Imagen profesional
- Automatización de procesos

**ROI estimado:** $400-700 USD/mes en automatización + cobros mejorados

### 💵 Resumen Costo-Beneficio

| Módulo | Esfuerzo (días) | Costo Desarrollo* | ROI Mensual | Recuperación |
|--------|----------------|-------------------|-------------|---------------|
| **Configuración** | 3-4 | $960-$1,280 | $200-$400 | 3-6 meses |
| **Reportes** | 4-5 | $1,280-$1,600 | $500-$800 | 2-3 meses |
| **Notificaciones** | 3-4 | $960-$1,280 | $400-$700 | 2-3 meses |
| **Usuarios** | 2-3 | $640-$960 | $100-$200 | 4-6 meses |
| **TOTAL** | **12-16 días** | **$3,840-$5,120** | **$1,200-$2,100** | **2-4 meses** |

*Costo estimado a $320 USD/día de desarrollo profesional

**Conclusión:** Inversión se recupera en **2-4 meses** con ROI mensual de **$1,200-2,100 USD**.

---

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
('sistema.nombre', 'WhatsApp Orders Manager', 'STRING', 'GENERAL', 'Nombre del sistema'),
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
    <title>Configuración - WhatsApp Orders Manager</title>
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

## ⏱️ ESTIMACIÓN TOTAL SPRINT 4

### Distribución de Tiempo

| Fase | Módulo | Días | Horas | % Sprint |
|------|--------|------|-------|----------|
| **Fase 1** | Configuración | 3-4 | 24-32 | 27% |
| **Fase 2** | Reportes | 4-5 | 32-40 | 33% |
| **Fase 3** | Notificaciones | 3-4 | 24-32 | 27% |
| **Fase 4** | Usuarios | 2-3 | 16-24 | 13% |
| **TOTAL** | | **12-16** | **96-128** | **100%** |

### Cronograma Propuesto (15 días laborables)

**Semana 1: Configuración + Inicio Reportes**
- Días 1-4: Configuración completa (BD, backend, frontend, testing)
- Día 5: Inicio de Reportes (BD + modelos)

**Semana 2: Reportes + Inicio Notificaciones**
- Días 6-9: Reportes (services, controllers, gráficas, exportación)
- Día 10: Testing Reportes + Inicio Notificaciones

**Semana 3: Notificaciones + Usuarios + Finalización**
- Días 11-13: Notificaciones completas
- Días 14-15: Usuarios avanzado
- Final: Testing integral, ajustes, documentación

---

## 📦 ENTREGABLES DEL SPRINT 4

### Documentación
- [ ] SPRINT_4_PLAN_MAESTRO.md (este documento)
- [ ] SPRINT_4_FASE_1_CONFIGURACION.md (detallado)
- [ ] SPRINT_4_FASE_2_REPORTES.md (detallado)
- [ ] SPRINT_4_FASE_3_NOTIFICACIONES.md (detallado)
- [ ] SPRINT_4_FASE_4_USUARIOS.md (detallado)
- [ ] SPRINT_4_CHECKLIST.md (seguimiento)
- [ ] SPRINT_4_RESUMEN.md (al finalizar)

### Código Backend
- [ ] 16 entidades nuevas
- [ ] 16 repositories
- [ ] 16 services (interfaces + impl)
- [ ] 12 controllers (web + REST)
- [ ] 20+ DTOs
- [ ] Migrations SQL (4 archivos)

### Código Frontend
- [ ] 12 vistas HTML (Thymeleaf)
- [ ] 30+ fragments HTML
- [ ] 15 archivos JavaScript
- [ ] 5 archivos CSS adicionales

### Testing
- [ ] 60+ tests unitarios
- [ ] 30+ tests de integración
- [ ] Tests E2E de flujos completos

**Total estimado:** ~150 archivos nuevos/modificados

---

## 🎯 CRITERIOS DE ÉXITO

### Fase 1: Configuración ✅
- [ ] Administrador puede editar datos de empresa
- [ ] Configuración de facturación funcional
- [ ] Envío de email de prueba exitoso
- [ ] Parámetros del sistema configurables
- [ ] Logo de empresa se muestra en facturas

### Fase 2: Reportes ✅
- [ ] 5 gráficas interactivas funcionando
- [ ] Exportación a PDF funcional
- [ ] Exportación a Excel funcional
- [ ] Filtros de fechas aplicando correctamente
- [ ] Dashboard de reportes responsive

### Fase 3: Notificaciones ✅
- [ ] Notificaciones en tiempo real en web
- [ ] Emails enviados automáticamente
- [ ] WhatsApp enviados por eventos
- [ ] Usuario puede configurar preferencias
- [ ] Historial de notificaciones visible

### Fase 4: Usuarios ✅
- [ ] CRUD completo de usuarios funcional
- [ ] Roles y permisos asignables
- [ ] Histórico de actividad registrado
- [ ] Bloqueo/desbloqueo de cuentas funcional
- [ ] Auditoría completa de acciones

---

## 🚀 PRÓXIMOS PASOS INMEDIATOS

### Acciones para HOY:
1. ✅ Revisar y aprobar este plan maestro
2. ⏸️ Crear estructura de carpetas para Sprint 4
3. ⏸️ Crear documento detallado FASE_1_CONFIGURACION.md
4. ⏸️ Ejecutar migración inicial de configuración
5. ⏸️ Iniciar desarrollo de entidades ConfiguracionEmpresa

### Acciones para MAÑANA:
1. ⏸️ Completar modelos y DTOs de configuración
2. ⏸️ Implementar repositories y services básicos
3. ⏸️ Crear controller de configuración
4. ⏸️ Diseñar mockup de vista de configuración

---

## 📝 NOTAS IMPORTANTES

### Dependencias Externas
- **Chart.js 4.x:** Para gráficas de reportes
- **Apache POI:** Para exportación Excel
- **iText 7:** Para exportación PDF
- **Spring Mail:** Para envío de emails
- **WebSocket:** Para notificaciones en tiempo real

### Consideraciones Técnicas
- Configuración debe ser **thread-safe** (caché)
- Reportes deben tener **caché de 5 minutos**
- Notificaciones deben procesarse **async**
- Exportaciones deben ser **sin bloqueo**

### Riesgos Identificados
⚠️ **Riesgo 1:** Configuración SMTP puede fallar (mitigation: validación previa)  
⚠️ **Riesgo 2:** Exportación PDF puede consumir memoria (mitigation: streaming)  
⚠️ **Riesgo 3:** Notificaciones en tiempo real requieren servidor (mitigation: usar polling si WebSocket falla)

---

**Creado por:** GitHub Copilot Agent  
**Fecha:** 21 de octubre de 2025  
**Versión:** 1.0  
**Estado:** 📝 PENDIENTE DE APROBACIÓN
