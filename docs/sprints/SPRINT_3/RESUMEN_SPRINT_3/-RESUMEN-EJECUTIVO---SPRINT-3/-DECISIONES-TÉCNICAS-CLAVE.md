## 🔗 DECISIONES TÉCNICAS CLAVE

### 1. Meta WhatsApp Business API vs Twilio
**Decisión:** Meta WhatsApp Business API  
**Razón:** 
- 1,000 conversaciones gratis/mes
- Ahorro de $96-132 USD/año (40%)
- Plantillas pre-aprobadas por Meta
- Mejor integración con WhatsApp oficial

**Documentado en:** `decisiones/DECISIONES_SPRINT_3.md`

---

### 2. Chats Ligados a Usuario (no Factura)
**Decisión:** Asociar mensajes a Usuario  
**Razón:**
- Mejor UX - conversaciones continuas
- Historial completo del cliente
- Permite múltiples pedidos en misma conversación

**Impacto:**
- Cambio en modelo `MensajeWhatsApp`
- Repository actualizado
- Requiere migración SQL

**Fecha:** 10 de noviembre de 2025

---

### 3. Chart.js 4.x para Dashboard
**Decisión:** Chart.js 4.x  
**Razón:**
- Librería ligera y moderna
- Excelente documentación
- Responsive por defecto
- Amplia compatibilidad

---

