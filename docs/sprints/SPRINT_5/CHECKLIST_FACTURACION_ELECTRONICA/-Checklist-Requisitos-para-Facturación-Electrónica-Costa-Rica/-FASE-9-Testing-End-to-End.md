## 📦 FASE 9: Testing End-to-End

**Estado:** ❌ **PENDIENTE**  
**Prioridad:** 🟠 **ALTA**  
**Tiempo estimado:** 3-4 horas

### Tareas:

#### 9.1 Datos de Prueba
- [ ] Configurar empresa con datos completos CR
- [ ] Crear 5 clientes de prueba (físicos y jurídicos)
- [ ] Crear 10 productos con códigos CABYS reales
- [ ] Configurar diferentes tarifas de IVA

#### 9.2 Escenarios de Prueba
- [ ] **Test 1:** Factura contado, efectivo, productos gravados 13%
- [ ] **Test 2:** Factura crédito 30 días, transferencia
- [ ] **Test 3:** Factura con productos exentos (tarifa 0%)
- [ ] **Test 4:** Factura con productos mixtos (gravados + exentos)
- [ ] **Test 5:** Factura en USD con tipo de cambio
- [ ] **Test 6:** Cliente persona física con DIMEX
- [ ] **Test 7:** Cliente jurídico con actividad económica

#### 9.3 Validaciones
- [ ] XML generado es válido contra XSD 4.4
- [ ] XML puede firmarse correctamente
- [ ] XML puede enviarse a Sandbox de Hacienda
- [ ] Respuesta de Hacienda es "Aceptado"
- [ ] Los datos en XML coinciden con factura en BD

#### 9.4 Pruebas de Integración
- [ ] Probar flujo completo: Crear factura → Enviar → Aceptar
- [ ] Probar reenvío automático en caso de error
- [ ] Probar consulta de estado
- [ ] Probar anulación de comprobante

---

