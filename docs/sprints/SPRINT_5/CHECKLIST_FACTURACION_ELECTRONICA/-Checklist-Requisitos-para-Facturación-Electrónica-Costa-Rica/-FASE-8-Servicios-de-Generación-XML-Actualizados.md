## 📦 FASE 8: Servicios de Generación XML Actualizados

**Estado:** ❌ **PENDIENTE**  
**Prioridad:** 🟠 **ALTA**  
**Tiempo estimado:** 4-5 horas

### Tareas:

#### 8.1 Actualizar XmlGeneracionService
- [ ] Abrir: `modules/facturacion/electronica/service/XmlGeneracionService.java`
- [ ] Agregar generación de `<CodigoActividadEmisor>`
- [ ] Agregar generación de `<CodigoActividadReceptor>` (opcional)
- [ ] Agregar generación de `<ProveedorSistemas>`
- [ ] Actualizar generación de `<Emisor>` con ubicación completa
- [ ] Actualizar generación de `<Receptor>` con ubicación completa
- [ ] Agregar generación de `<CondicionVenta>`
- [ ] Agregar generación de `<MedioPago>`
- [ ] Agregar generación de `<CodigoTipoMoneda>`

#### 8.2 Actualizar Generación de Líneas de Detalle
- [ ] Agregar `<CodigoCABYS>` en cada línea
- [ ] Agregar `<UnidadMedida>` en cada línea
- [ ] Actualizar estructura de `<Impuesto>`:
  - [ ] `<Codigo>01</Codigo>` (IVA)
  - [ ] `<CodigoTarifaIVA>08</CodigoTarifaIVA>` (13%)
  - [ ] `<Tarifa>13.00</Tarifa>`
  - [ ] `<Monto>` calculado

#### 8.3 Actualizar Cálculos de Impuestos
- [ ] Separar cálculo por tipo: Gravado, Exento, Exonerado
- [ ] Generar `<TotalGravado>`, `<TotalExento>`, `<TotalExonerado>`
- [ ] Calcular correctamente `<TotalImpuesto>` por tarifa

#### 8.4 Validación Contra XSD
- [ ] Validar XML generado contra XSD 4.4
- [ ] Crear tests unitarios con datos reales
- [ ] Validar todos los campos obligatorios presentes
- [ ] Validar formatos numéricos (5 decimales en montos)

---

