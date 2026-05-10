## ✅ CHECKLIST DE IMPLEMENTACIÓN

### FASE 1: Migraciones de Base de Datos
- [ ] Script SQL para campos de `empresa`
- [ ] Script SQL para campos de `cliente`
- [ ] Script SQL para campos de `factura`
- [ ] Script SQL para campos de `producto`
- [ ] Ejecutar migraciones en entorno de desarrollo
- [ ] Validar estructura con `SHOW CREATE TABLE`

### FASE 2: Actualizar Entidades JPA
- [ ] Agregar campos en `Empresa.java`
- [ ] Agregar campos en `Cliente.java`
- [ ] Agregar campos en `Factura.java`
- [ ] Agregar campos en `Producto.java`
- [ ] Crear enums: `TipoIdentificacion`, `CondicionVenta`, `MedioPago`, `CodigoTarifaIVA`

### FASE 3: Actualizar DTOs y Mappers
- [ ] Actualizar DTOs de transferencia
- [ ] Actualizar mappers de conversión
- [ ] Validar serialización JSON

### FASE 4: Actualizar Formularios Web
- [ ] Form de configuración empresa (códigos Hacienda)
- [ ] Form de clientes (tipo identificación, ubicación)
- [ ] Form de productos (CABYS, unidad medida, IVA)
- [ ] Form de facturas (condición venta, medio pago)

### FASE 5: Lógica de Generación XML
- [ ] Servicio de generación XML con nuevos campos
- [ ] Validación contra XSD 4.4
- [ ] Pruebas con datos reales
- [ ] Integración con servicio de firma digital

### FASE 6: Testing
- [ ] Unit tests de entidades
- [ ] Integration tests de generación XML
- [ ] Validación con ejemplos reales de Hacienda
- [ ] Pruebas end-to-end del flujo completo

---

