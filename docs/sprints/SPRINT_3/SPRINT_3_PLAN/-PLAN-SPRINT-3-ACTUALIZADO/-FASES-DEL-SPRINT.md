## 📋 FASES DEL SPRINT

### FASE 0: PREPARACIÓN META WHATSAPP (PARALELA) 🏃
**Duración:** 3-7 días (21-27 octubre)  
**Prioridad:** CRÍTICA ⚡  
**Estado:** 🔴 NO INICIADO

> Esta fase se ejecuta en paralelo mientras se desarrollan otros componentes

#### 0.1 Configuración Cuenta Meta (Día 1 - 21 oct)
- [ ] Crear cuenta Facebook Business
  - URL: https://business.facebook.com
  - Verificar email y teléfono
- [ ] Crear aplicación en Meta for Developers
  - URL: https://developers.facebook.com
  - Agregar producto "WhatsApp"
- [ ] Configurar información del negocio
  - Nombre comercial
  - Dirección
  - Categoría (Software/ERP)

#### 0.2 Verificación Número WhatsApp (Día 1-2)
- [ ] Agregar número de teléfono
- [ ] Verificar vía SMS
- [ ] Obtener Phone Number ID
- [ ] Obtener Access Token (temporal)

#### 0.3 Creación de Plantillas (Día 2-3)
- [ ] **Plantilla 1:** `factura_generada`
  ```
  Hola {{1}}, tu factura #{{2}} por ${{3}} ha sido generada.
  Vence: {{4}}. Ver detalles: {{5}}
  ```
- [ ] **Plantilla 2:** `recordatorio_pago`
  ```
  Hola {{1}}, recordatorio: La factura #{{2}} por ${{3}} vence {{4}}.
  Puedes pagar en: {{5}}
  ```
- [ ] **Plantilla 3:** `pago_recibido`
  ```
  ¡Pago confirmado! Gracias {{1}}, recibimos ${{2}} por factura #{{3}}.
  ```
- [ ] **Plantilla 4:** `factura_vencida`
  ```
  Hola {{1}}, la factura #{{2}} por ${{3}} está vencida desde {{4}}.
  Por favor realiza el pago a la brevedad.
  ```
- [ ] **Plantilla 5:** `bienvenida_cliente`
  ```
  ¡Bienvenido {{1}}! Gracias por confiar en nosotros.
  Recibirás notificaciones de tus facturas por este medio.
  ```

#### 0.4 Aprobación y Verificación (Día 3-7)
- [ ] Enviar plantillas a revisión de Meta
- [ ] Esperar aprobación (48-96 horas típicamente)
- [ ] Generar Access Token permanente
- [ ] Configurar webhook URL
- [ ] Verificar webhook con token
- [ ] **Checkpoint:** ✅ Cuenta aprobada y operativa

**Entregables Fase 0:**
- ✅ Cuenta Meta WhatsApp Business verificada
- ✅ 5 plantillas aprobadas
- ✅ Access Token permanente
- ✅ Phone Number ID
- ✅ Webhook configurado

---

### FASE 1: INTEGRACIÓN WHATSAPP API (CRÍTICA) ⭐
**Duración:** 5-7 días (28 oct - 5 nov)  
**Prioridad:** MÁXIMA  
**Estado:** ⏸️ ESPERANDO FASE 0

#### 1.1 Backend - Modelos y Persistencia (6h)
- [ ] Crear migración `MIGRATION_WHATSAPP_SPRINT_3.sql`
- [ ] Crear tabla `mensaje_whatsapp`
  ```sql
  CREATE TABLE mensaje_whatsapp (
    id_mensaje BIGINT PRIMARY KEY AUTO_INCREMENT,
    telefono VARCHAR(20) NOT NULL,
    mensaje TEXT,
    tipo ENUM('ENVIADO', 'RECIBIDO'),
    estado ENUM('PENDIENTE', 'ENVIADO', 'ENTREGADO', 'LEIDO', 'FALLIDO'),
    id_factura BIGINT,
    id_mensaje_whatsapp VARCHAR(255), -- ID de Meta
    fecha_envio TIMESTAMP,
    fecha_entrega TIMESTAMP,
    fecha_lectura TIMESTAMP,
    error TEXT,
    
    INDEX idx_telefono_fecha (telefono, fecha_envio),
    INDEX idx_factura (id_factura),
    INDEX idx_estado (estado),
    FOREIGN KEY (id_factura) REFERENCES factura(id_factura)
  );
  ```
- [ ] Crear tabla `plantilla_whatsapp`
  ```sql
  CREATE TABLE plantilla_whatsapp (
    id_plantilla INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    codigo_meta VARCHAR(50) NOT NULL, -- Nombre en Meta
    contenido TEXT NOT NULL,
    parametros JSON, -- Array de nombres de parámetros
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  ```
- [ ] Crear entidad `MensajeWhatsApp.java`
- [ ] Crear entidad `PlantillaWhatsApp.java`
- [ ] Crear `MensajeWhatsAppRepository.java`
- [ ] Crear `PlantillaWhatsAppRepository.java`

#### 1.2 Backend - DTOs (4h)
- [ ] Crear `MetaWebhookRequest.java` (estructura completa de webhook)
- [ ] Crear `MetaWhatsAppResponse.java`
- [ ] Crear `EnviarMensajeRequest.java`
- [ ] Crear `MensajeWhatsAppDTO.java`

#### 1.3 Backend - Servicios Core (12h)
- [ ] Crear interfaz `WhatsAppService.java`
  ```java
  public interface WhatsAppService {
      String enviarMensaje(String telefono, String mensaje);
      String enviarMensajeConPlantilla(String telefono, String plantillaId, Map<String, String> params);
      String enviarDocumento(String telefono, String urlDocumento);
      String obtenerEstadoMensaje(String idMensaje);
  }
  ```
- [ ] Crear `MetaWhatsAppServiceImpl.java`
  - Implementar envío de mensaje simple
  - Implementar envío con plantilla
  - Implementar envío de documento
  - Integración con Graph API v18.0
  - Manejo de errores y reintentos
  - Logging detallado
- [ ] Crear `MensajeWhatsAppServiceImpl.java`
  - Guardar mensajes enviados/recibidos
  - Actualizar estados (entregado, leído)
  - Queries y reportes
  - Procesamiento asíncrono

#### 1.4 Backend - Webhook Controller (8h)
- [ ] Crear `WhatsAppWebhookController.java`
  - `GET /api/whatsapp/webhook` → Verificación Meta
  - `POST /api/whatsapp/webhook` → Recepción mensajes
  - Validación de firma (seguridad)
  - Respuesta < 200ms
  - Procesamiento asíncrono con @Async
- [ ] Implementar procesamiento de estados
  - Mensaje enviado
  - Mensaje entregado
  - Mensaje leído
  - Mensaje fallido
- [ ] Implementar procesamiento de mensajes entrantes
  - Detectar comandos (SALDO, FACTURAS, etc.)
  - Respuestas automáticas
  - Logging

#### 1.5 Backend - Integración con Facturación (8h)
- [ ] Test: Recepción de webhooks
- [ ] Test: Manejo de errores (número inválido, API caída)

---

### FASE 2: DASHBOARD AVANZADO (2-3 días)
**Duración:** 16-20 horas  
**Prioridad:** ALTA

#### 2.1 Backend - Servicios de Estadísticas (6-8h)
- [ ] Crear `DashboardService.java`
- [ ] Crear `EstadisticasService.java`
- [ ] Implementar métodos:
  - `getVentasMensuales(int meses)` → Últimos N meses
  - `getTopProductos(int limit)` → Top N productos
  - `getTopClientes(int limit)` → Top N clientes
  - `getEstadoFacturas()` → Pagadas, Pendientes, Vencidas
  - `getComparativaAnual()` → Año actual vs. anterior
  - `getKPIs()` → Indicadores clave

#### 2.2 Backend - Stored Procedures (4-5h)
- [ ] `SP_GetVentasMensuales`
- [ ] `SP_GetTopProductos`
- [ ] `SP_GetTopClientes`
- [ ] `SP_GetEstadoFacturas`
- [ ] `SP_GetComparativaAnual`
- [ ] `SP_GetIngresosPorDivisa`
- [ ] `SP_GetTasaCobro`
- [ ] `SP_GetCrecimientoClientes`

#### 2.3 Backend - API Controllers (2-3h)
- [ ] Crear `DashboardApiController.java`
  - `GET /api/dashboard/ventas-mensuales`
  - `GET /api/dashboard/top-productos`
  - `GET /api/dashboard/top-clientes`
  - `GET /api/dashboard/estado-facturas`
  - `GET /api/dashboard/kpis`

#### 2.4 Frontend - Vistas (4-6h)
- [ ] Actualizar `templates/dashboard/dashboard.html`
  - Sección de KPIs (4 cards principales)
  - Contenedor para gráficas
- [ ] Crear layouts para gráficas

#### 2.5 Frontend - Chart.js Integration (6-8h)
- [ ] Configurar Chart.js en el proyecto
- [ ] Crear `static/js/dashboard-charts.js`
- [ ] Implementar gráficas:
  1. **Line Chart:** Ventas mensuales (últimos 12 meses)
  2. **Pie Chart:** Estado de facturas
  3. **Bar Chart:** Top 5 productos más vendidos
  4. **Doughnut Chart:** Nuevos vs. clientes recurrentes
  5. **Mixed Chart:** Ingresos vs. gastos
- [ ] Crear `static/css/dashboard-v2.css`
- [ ] Diseño responsive de gráficas

---

### FASE 3: SISTEMA MULTI-DIVISA (5-7 días)
**Duración:** 38-52 horas  
**Prioridad:** ALTA

#### 3.1 Base de Datos (6-8h)
- [ ] Crear tabla `divisa`
  - Campos: id, codigo, nombre, simbolo, es_maestra, activo, decimales, pais
- [ ] Crear tabla `tipo_cambio`
  - Campos: id, id_divisa_origen, id_divisa_destino, fecha, tasa, fuente
- [ ] Modificar tabla `factura`
  - Agregar: id_divisa, tipo_cambio_registrado, total_divisa_maestra
- [ ] Crear triggers de conversión automática
- [ ] Insertar divisas iniciales (MXN, USD, EUR, GTQ, HNL)
- [ ] Crear índices optimizados

#### 3.2 Backend - Modelos (4-6h)
- [ ] Crear entidad `Divisa.java`
- [ ] Crear entidad `TipoCambio.java`
- [ ] Actualizar entidad `Factura.java`
- [ ] Crear DTOs: `DivisaDTO`, `TipoCambioDTO`
- [ ] Implementar validaciones

#### 3.3 Backend - Repositories (2-3h)
- [ ] Crear `DivisaRepository.java`
  - `findByCodigo(String codigo)`
  - `findDivisaMaestra()`
  - `findAllActivas()`
- [ ] Crear `TipoCambioRepository.java`
  - `findTipoCambioVigente(divisaOrigen, divisaDestino, fecha)`
  - `findHistorico(divisaOrigen, divisaDestino, limit)`

#### 3.4 Backend - Servicios (8-10h)
- [ ] Crear `DivisaService.java` + Impl
  - CRUD completo
  - Validar una sola divisa maestra
  - Método: `establecerDivisaMaestra(idDivisa)`
- [ ] Crear `TipoCambioService.java` + Impl
  - CRUD completo
  - Método: `findTipoCambioActual(divisaOrigen, divisaDestino)`
  - Método: `convertir(monto, divisaOrigen, divisaDestino, fecha)`
  - Validar tipos de cambio duplicados
- [ ] Actualizar `FacturaService.java`
  - Agregar lógica de conversión automática
  - Registrar tipo de cambio al crear factura

#### 3.5 Backend - Controllers (6-8h)
- [ ] Crear `DivisaController.java`
  - CRUD completo de divisas
  - Establecer divisa maestra
- [ ] Crear `TipoCambioController.java`
  - CRUD completo de tipos de cambio
  - Obtener TC actual
  - Ver histórico
- [ ] Actualizar `FacturaController.java`
  - Enviar lista de divisas al formulario
  - Obtener TC automáticamente al seleccionar divisa

#### 3.6 Frontend - Gestión de Divisas (4-6h)
- [ ] Vista: `templates/configuracion/divisas.html`
  - Tabla de divisas
  - Formulario crear/editar divisa
  - Indicador de divisa maestra
- [ ] Vista: `templates/configuracion/tipos-cambio.html`
  - Formulario registro de TC
  - Tabla histórico
  - Gráfica de evolución (opcional)
- [ ] Crear `static/js/divisas.js`
- [ ] Crear `static/js/tipos-cambio.js`

#### 3.7 Frontend - Facturación (4-6h)
- [ ] Actualizar `templates/facturas/add-form.html`
  - Agregar selector de divisa
  - Mostrar tipo de cambio actual
  - Cálculo en tiempo real
- [ ] Actualizar modal de detalle de factura
  - Mostrar divisa original
  - Mostrar conversión a divisa maestra
  - Mostrar tipo de cambio usado
- [ ] Actualizar tabla de facturas
  - Columna de divisa
  - Filtro por divisa

#### 3.8 Reportes Consolidados (4-6h)
- [ ] Actualizar `ReporteService.java`
  - Usar `total_divisa_maestra` para consolidar
  - Método: `calcularVentasPorDivisa()`
- [ ] Actualizar vistas de reportes
  - Mostrar totales en divisa maestra
  - Desglose por divisa original
- [ ] Agregar filtro por divisa en reportes

#### 3.9 Testing Multi-Divisa (6-8h)
- [ ] Test: Crear factura en USD → Se convierte a MXN
- [ ] Test: Crear factura en MXN → No se convierte
- [ ] Test: Registrar tipo de cambio
- [ ] Test: Histórico de tipos de cambio
- [ ] Test: Reporte consolidado usa divisa maestra
- [ ] Test: Cambiar divisa maestra
- [ ] Test: Validar una sola divisa maestra
- [ ] Test: Triggers funcionan correctamente

---

### FASE 4: MEJORAS DE AUTENTICACIÓN (1-2 días)
**Duración:** 8-11 horas  
**Prioridad:** MEDIA

#### 4.1 Username en lugar de Teléfono (4-6h)
- [ ] Agregar campo `username` a tabla `usuario`
- [ ] Migración SQL para poblar usernames
- [ ] Actualizar `Usuario.java`
- [ ] Actualizar `UsuarioRepository.java`
- [ ] Actualizar `UserDetailsServiceImpl.java`
- [ ] Actualizar `SecurityConfig.java`
- [ ] Actualizar vista de login
- [ ] Actualizar vista de registro
- [ ] Testing completo

#### 4.2 Remember Me (1-2h)
- [ ] Configurar en `SecurityConfig.java`
- [ ] Agregar checkbox en login
- [ ] Testing de sesión persistente

#### 4.3 Migrar Timestamp a LocalDateTime (2-3h)
- [ ] Actualizar `Usuario.java`
- [ ] Actualizar `UserDetailsServiceImpl.java`
- [ ] Actualizar templates (usar #temporals)
- [ ] Testing

---

### FASE 5: MEJORAS DE REPORTES (1 día)
**Duración:** 6-8 horas  
**Prioridad:** BAJA (Opcional)

#### 5.1 Diseño Mejorado de PDF (4-5h)
- [ ] Agregar logo de empresa
- [ ] Mejorar diseño de facturas PDF
- [ ] Agregar gráficas en reportes PDF
- [ ] Pie de página con información fiscal

#### 5.2 Excel Avanzado (2-3h)
- [ ] Múltiples hojas en un mismo archivo
- [ ] Formato de celdas mejorado
- [ ] Gráficas en Excel

---

