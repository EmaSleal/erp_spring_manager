## 📦 FASE 2: CONFIGURACIÓN DE FACTURACIÓN

### 2.1 Modelo y Base de Datos

☑ 2.1.1 Crear modelo ConfiguracionFacturacion.java
      - Campos: serie, número, prefijo, IGV, moneda, etc.
      - Validaciones
      - Métodos de negocio: generarNumeroFactura(), calcularIgv(), calcularTotal()
      - Valores por defecto: F001, 18%, PEN, S/
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.1.2 Crear script SQL
      - CREATE TABLE configuracion_facturacion
      - INSERT configuración por defecto
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Opcional - Hibernate DDL auto lo maneja

☑ 2.1.3 Ejecutar migración
      - Verificar tabla
      - Verificar datos
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Hibernate crea tabla automáticamente

### 2.2 Capa de Datos

☑ 2.2.1 Crear ConfiguracionFacturacionRepository.java
      - findConfiguracionActiva()
      - existeConfiguracionActiva()
      - findBySerieFactura()
      - contarConfiguracionesActivas()
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.2.2 Crear ConfiguracionFacturacionService.java
      - getConfiguracionActiva()
      - getOrCreateConfiguracion()
      - save() / update()
      - generarSiguienteNumeroFactura()
      - incrementarNumeroFactura()
      - validarConfiguracion()
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 2.3 Integración

☑ 2.3.1 Actualizar modelo Factura.java
      - Agregar campos: numeroFactura, serie, subtotal, igv
      - Constraint UNIQUE en numeroFactura
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.3.2 Actualizar FacturaServiceImpl.java
      - Inyectar ConfiguracionFacturacionService
      - Auto-generar número de factura
      - Aplicar IGV según configuración
      - Calcular total automáticamente
      - Incrementar número después de guardar
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.3.3 Actualizar FacturaRepository.java
      - findByNumeroFactura()
      - existsByNumeroFactura()
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 2.4 Controller y Vistas

☑ 2.4.1 Actualizar ConfiguracionController.java
      - GET /configuracion/facturacion
      - POST /configuracion/facturacion/guardar
      - Cargar configuración existente
      - Validaciones
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.4.2 Crear vista configuracion/facturacion.html
      - Formulario de configuración completo
      - Campos: serie, número, IGV, moneda, términos
      - Preview de numeración en tiempo real
      - Validaciones HTML5
      - Sidebar con ayuda
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.4.3 Actualizar configuracion/index.html
      - Habilitar tab "Facturación"
      - Cargar fragment facturacionForm
      - Tab activo según parámetro
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.4.4 Actualizar configuracion.js
      - Función actualizarPreview()
      - Validación de formato de número
      - Validación de moneda ISO 4217
      - Preview en tiempo real
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 2.4.5 Testing de integración
      - Crear configuración de facturación
      - Verificar numeración automática
      - Verificar cálculo de IGV
      - Crear facturas con nueva configuración
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Listo para testing manual

□ 2.3.3 Testing de integración
      - Crear factura con nueva configuración
      - Verificar numeración automática
      - Verificar cálculo de IGV
      
      Estado: □ Pendiente  □ En progreso  □ Completado

---

