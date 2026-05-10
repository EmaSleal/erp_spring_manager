## 📝 PRÓXIMOS PASOS

### Punto 2.4: Controller y Vistas

**Tareas pendientes:**

1. **Actualizar ConfiguracionController.java**
   - Agregar endpoint: `GET /configuracion/facturacion`
   - Agregar endpoint: `POST /configuracion/facturacion/guardar`
   - Cargar configuración existente en formulario

2. **Crear configuracion/facturacion.html**
   - Formulario con todos los campos de configuración
   - Preview en vivo del formato de número
   - Validaciones client-side
   - Ayudas contextuales

3. **Actualizar configuracion/index.html**
   - Habilitar tab "Facturación"
   - Link a vista de configuración

4. **Actualizar configuracion.js**
   - Preview dinámico de numeración
   - Validación de formato
   - AJAX para guardar configuración

5. **Actualizar FacturaController.java**
   - Mostrar preview de número en formulario
   - Pasar configuración a vista (símbolo moneda)
   - Validar subtotal antes de calcular

6. **Actualizar facturas/add-form.html**
   - Mostrar próximo número de factura (read-only)
   - Mostrar símbolo de moneda configurado
   - Campo subtotal con cálculo automático de IGV

---

