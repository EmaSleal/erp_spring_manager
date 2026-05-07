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
