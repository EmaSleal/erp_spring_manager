## 🚀 PRÓXIMOS PASOS

### Fase 4 cerrada ✅ — 2026-05-09

La integración con Hacienda CR quedó completamente funcional en sandbox.

### Pendiente para sprints futuros

- Migrar de **sandbox → producción** (certificado PKCS12 real + URL producción)
- Agregar **job `@Scheduled`** para recuperar comprobantes en estado ENVIADO por más de 10 minutos (edge case de downtime de Hacienda)
- Evaluar tests unitarios para `XmlGeneratorServiceImpl` y `FirmaDigitalServiceImpl` cuando la lógica esté estabilizada

---

**Fase creada:** 16 de enero de 2026  
**Fase cerrada:** 09 de mayo de 2026  
**Responsable:** Emanuel Soto
