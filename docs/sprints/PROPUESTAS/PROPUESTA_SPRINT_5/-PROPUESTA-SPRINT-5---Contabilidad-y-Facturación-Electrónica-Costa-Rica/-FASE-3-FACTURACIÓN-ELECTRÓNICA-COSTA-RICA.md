## 🇨🇷 FASE 3: FACTURACIÓN ELECTRÓNICA COSTA RICA

**Duración:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Progreso estimado:** 0/52 tareas

### Objetivos

- Integración con Hacienda CR (API v4.4)
- Generación de XML según anexo v4.4
- Firma digital XAdES-EPES
- Envío a Hacienda (RECEPCION API)
- Consulta de estado y callbacks
- Almacenamiento de comprobantes y respuestas

### 3.1 Base de Datos (8 tareas)

#### Actualizar tabla `factura`
```sql
-- Agregar campos para facturación electrónica CR
ALTER TABLE factura ADD COLUMN (
    -- Consecutivo Hacienda (20 dígitos)
    consecutivo_hacienda VARCHAR(20) UNIQUE COMMENT 'SSS-TTTTT-TT-NNNNNNNNNN',
    
    -- Clave numérica (50 dígitos)
    clave_hacienda VARCHAR(50) UNIQUE COMMENT 'Clave única de 50 dígitos',
    
    -- Datos adicionales para FE
    condicion_venta VARCHAR(2) DEFAULT '01' COMMENT '01=Contado, 02=Crédito, etc.',
    plazo_credito INT COMMENT 'Días de crédito si aplica',
    medio_pago VARCHAR(2) DEFAULT '01' COMMENT 'Código catálogo Hacienda',
    
    -- Estado Hacienda
    estado_hacienda ENUM(
        'BORRADOR', 
        'FIRMADO', 
        'ENVIADO', 
        'RECIBIDO', 
        'ACEPTADO', 
        'RECHAZADO', 
        'ERROR'
    ) DEFAULT 'BORRADOR',
    
    fecha_envio_hacienda DATETIME COMMENT 'Cuándo se envió a Hacienda',
    fecha_respuesta_hacienda DATETIME COMMENT 'Cuándo respondió Hacienda',
    
    -- Almacenamiento de XMLs
    xml_sin_firma LONGTEXT COMMENT 'XML generado antes de firmar',
    xml_firmado LONGTEXT COMMENT 'XML firmado (el que se envía)',
    xml_respuesta LONGTEXT COMMENT 'Respuesta de Hacienda (base64)',
    
    -- Trazabilidad
    location_hacienda VARCHAR(255) COMMENT 'URL para consultar estado',
    codigo_error VARCHAR(10) COMMENT 'Código de error de Hacienda si aplica',
    mensaje_error TEXT COMMENT 'Mensaje de error detallado',
    
    -- Reintentos
    intentos_envio INT DEFAULT 0,
    ultimo_intento DATETIME
);

-- Índices para búsquedas
CREATE INDEX idx_factura_clave_hacienda ON factura(clave_hacienda);
CREATE INDEX idx_factura_estado_hacienda ON factura(estado_hacienda);
CREATE INDEX idx_factura_consecutivo_hacienda ON factura(consecutivo_hacienda);
```

#### Actualizar tabla `linea_factura`
```sql
ALTER TABLE linea_factura ADD COLUMN (
    -- CABYS (13 dígitos obligatorio v4.4)
    cabys VARCHAR(13) COMMENT 'Código de bienes y servicios',
    
    -- Unidad de medida (catálogo Hacienda)
    unidad_medida VARCHAR(5) DEFAULT 'Unid' COMMENT 'Sp, Kg, m, etc.',
    
    -- Impuestos detallados
    codigo_impuesto VARCHAR(2) DEFAULT '01' COMMENT '01=IVA, 02=Selectivo, etc.',
    codigo_tarifa VARCHAR(2) DEFAULT '08' COMMENT '08=13%, 01=0%, etc.',
    tarifa_impuesto DECIMAL(5, 2) DEFAULT 13.00 COMMENT 'Porcentaje de impuesto',
    monto_impuesto DECIMAL(10, 2) COMMENT 'Monto calculado del impuesto',
    
    -- Naturaleza de la línea
    naturaleza_descuento VARCHAR(100) COMMENT 'Razón del descuento si aplica'
);
```

#### Tabla: `configuracion_hacienda`
```sql
CREATE TABLE configuracion_hacienda (
    id_configuracion INT PRIMARY KEY AUTO_INCREMENT,
    
    -- Configuración única (Singleton)
    ambiente ENUM('SANDBOX', 'PRODUCCION') DEFAULT 'SANDBOX',
    
    -- URLs
    url_token VARCHAR(255) NOT NULL COMMENT 'URL del IdP para obtener token',
    url_recepcion VARCHAR(255) NOT NULL COMMENT 'URL de recepción de comprobantes',
    
    -- Credenciales (encriptadas)
    client_id VARCHAR(100) DEFAULT 'api-prod',
    username VARCHAR(100) COMMENT 'Usuario de ATV',
    password_encrypted VARCHAR(255) COMMENT 'Contraseña ATV encriptada',
    
    -- Certificado de firma
    ruta_certificado VARCHAR(255) COMMENT 'Ruta al archivo .p12',
    password_certificado_encrypted VARCHAR(255),
    
    -- Configuración de emisor
    tipo_identificacion_emisor VARCHAR(2) DEFAULT '02' COMMENT '01=Física, 02=Jurídica',
    numero_identificacion_emisor VARCHAR(12) NOT NULL COMMENT 'Cédula padded a 12',
    nombre_comercial_emisor VARCHAR(80),
    
    -- Consecutivos
    sucursal VARCHAR(3) DEFAULT '001' COMMENT 'Código de sucursal (3 dígitos)',
    terminal VARCHAR(5) DEFAULT '00001' COMMENT 'Código de terminal (5 dígitos)',
    consecutivo_actual INT DEFAULT 0 COMMENT 'Último consecutivo utilizado',
    
    -- Control
    activo BOOLEAN DEFAULT TRUE,
    modo_contingencia BOOLEAN DEFAULT FALSE COMMENT 'Activar si no hay conexión con Hacienda',
    
    -- Callback
    url_callback VARCHAR(255) COMMENT 'URL donde Hacienda enviará respuestas',
    
    -- Auditoría
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Configuración para integración con Hacienda CR';

-- Datos iniciales (SANDBOX)
INSERT INTO configuracion_hacienda (
    ambiente, 
    url_token, 
    url_recepcion,
    tipo_identificacion_emisor,
    numero_identificacion_emisor,
    nombre_comercial_emisor
) VALUES (
    'SANDBOX',
    'https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token',
    'https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1/recepcion',
    '02',
    '000000000000',
    'EMPRESA DEMO CR'
);
```

**Tareas BD:**
- [ ] 3.1.1 - Migración: Alterar `factura` con campos FE
- [ ] 3.1.2 - Migración: Alterar `linea_factura` con CABYS e impuestos
- [ ] 3.1.3 - Crear tabla `configuracion_hacienda` con datos SANDBOX
- [ ] 3.1.4 - Crear tabla `log_envio_hacienda` (auditoría completa)
- [ ] 3.1.5 - Crear función SQL para generar consecutivo (20 dígitos)
- [ ] 3.1.6 - Crear función SQL para generar clave (50 dígitos)
- [ ] 3.1.7 - Crear trigger para auto-generar consecutivo y clave
- [ ] 3.1.8 - Script de rollback completo

### 3.2 Backend - Modelos (6 tareas)

- [ ] 3.2.1 - Crear entidad `ConfiguracionHacienda.java`
- [ ] 3.2.2 - Crear DTO `FacturaElectronicaDTO.java` (para XML)
- [ ] 3.2.3 - Crear enum `EstadoHacienda.java`
- [ ] 3.2.4 - Crear enum `CondicionVenta.java` (catálogo CR)
- [ ] 3.2.5 - Crear enum `MedioPago.java` (catálogo CR)
- [ ] 3.2.6 - Actualizar `Factura.java` con nuevos campos

### 3.3 Backend - Generación de XML (8 tareas)

- [ ] 3.3.1 - Crear `XmlGeneratorService.java` para construir XML v4.4
- [ ] 3.3.2 - Implementar método `generarXmlFactura(Factura factura)`
- [ ] 3.3.3 - Agregar dependencia JAXB para marshalling XML
- [ ] 3.3.4 - Crear clases JAXB según schema v4.4 (o usar JAXBContext manual)
- [ ] 3.3.5 - Implementar validación contra XSD v4.4
- [ ] 3.3.6 - Calcular campos automáticos (resumen impuestos, totales)
- [ ] 3.3.7 - Generar estructura de Emisor desde `ConfiguracionEmpresa`
- [ ] 3.3.8 - Generar estructura de Receptor desde `Cliente`

### 3.4 Backend - Firma Digital (5 tareas)

- [ ] 3.4.1 - Crear `FirmaDigitalService.java` para XAdES-EPES
- [ ] 3.4.2 - Agregar dependencia `xmlsec` para firma XML
- [ ] 3.4.3 - Implementar método `firmarXml(String xml)` → XML firmado
- [ ] 3.4.4 - Cargar certificado .p12 desde configuración
- [ ] 3.4.5 - Validar certificado (fecha de expiración, cadena de confianza)

### 3.5 Backend - Integración API Hacienda (10 tareas)

- [ ] 3.5.1 - Crear `HaciendaApiService.java` para comunicación HTTP
- [ ] 3.5.2 - Implementar `TokenManager` (cachea token 5 min, renueva antes de expirar)
- [ ] 3.5.3 - Método: `obtenerToken()` usando OAuth2 Resource Owner Password
- [ ] 3.5.4 - Configurar `WebClient` con interceptores (token, UTF-8, logging)
- [ ] 3.5.5 - Método: `enviarComprobante(String clave, String xmlBase64)` → POST /recepcion
- [ ] 3.5.6 - Método: `consultarEstado(String clave)` → GET /recepcion/{clave}
- [ ] 3.5.7 - Implementar callback endpoint para recibir respuesta de Hacienda
- [ ] 3.5.8 - Manejo de errores HTTP (401, 429 rate limit, 500)
- [ ] 3.5.9 - Circuit breaker con Resilience4j (si Hacienda cae)
- [ ] 3.5.10 - Implementar cola de reintentos con exponential backoff

### 3.6 Backend - Workflow Completo (8 tareas)

- [ ] 3.6.1 - Crear `FacturaElectronicaService.java` (orquestador)
- [ ] 3.6.2 - Método: `firmarYEnviarFactura(Integer idFactura)`
  1. Generar consecutivo y clave
  2. Generar XML v4.4
  3. Firmar XML
  4. Enviar a Hacienda
  5. Guardar XMLs y respuesta
  6. Actualizar estado factura
- [ ] 3.6.3 - Método: `consultarYActualizarEstado(Integer idFactura)`
- [ ] 3.6.4 - Método: `reprocesarFacturasRechazadas()`
- [ ] 3.6.5 - Scheduled task para consultar estados pendientes
- [ ] 3.6.6 - Integración con `NotificacionService` (factura aceptada/rechazada)
- [ ] 3.6.7 - Generar PDF de factura con código QR (clave Hacienda)
- [ ] 3.6.8 - Enviar email al cliente con XML + PDF adjuntos

### 3.7 Frontend - Vistas (4 tareas)

- [ ] 3.7.1 - Crear `templates/modules/facturacion/configuracion-hacienda.html`
- [ ] 3.7.2 - Actualizar `facturacion/detalle.html` con badge de estado Hacienda
- [ ] 3.7.3 - Botón "Firmar y Enviar a Hacienda" en detalle de factura
- [ ] 3.7.4 - Modal de historial de envíos y reintentos

### 3.8 Testing (3 tareas)

- [ ] 3.8.1 - Tests unitarios `XmlGeneratorServiceTest`
- [ ] 3.8.2 - Test de generación de XML válido contra XSD
- [ ] 3.8.3 - Integration test con SANDBOX de Hacienda

---

