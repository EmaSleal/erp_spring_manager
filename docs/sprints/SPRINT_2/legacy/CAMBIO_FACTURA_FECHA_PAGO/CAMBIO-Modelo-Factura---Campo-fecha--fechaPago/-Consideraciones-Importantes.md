## ⚠️ Consideraciones Importantes

### **1. Datos Legacy**
- Facturas antiguas tendrán `fecha_pago` calculada automáticamente
- Revisar si las fechas calculadas son correctas
- Puede requerir ajuste manual para casos especiales

### **2. Días de Crédito**
- Actualmente hardcoded: 7 días
- Considerar hacer configurable por cliente o tipo de factura
- Futuro: agregar campo `diasCredito` en Cliente o ConfiguracionFacturacion

### **3. Validaciones**
- `fecha_pago` debe ser >= `fecha_entrega`
- Si se edita `fecha_entrega`, recalcular `fecha_pago`
- Alertar si `fecha_pago` < `fecha_actual` (factura vencida)

---

**Autor:** GitHub Copilot  
**Fecha:** 13 de octubre de 2025  
**Sprint:** Sprint 2 - Fase 5 (Notificaciones)  
**Estado:** ✅ MODELO ACTUALIZADO - Pendiente migración SQL
