## ⚠️ CAMBIOS IMPORTANTES

### 🔄 Refactorización: Chats Ligados a Usuario (10 nov 2025)
**Decisión Técnica:** Los chats de WhatsApp están ligados a **Usuario** (no a Factura/Pedido)

**Razón:** Mejor experiencia de usuario - conversaciones continuas que pueden abarcar múltiples pedidos

**Impacto:**
- ✅ Modelo `MensajeWhatsApp` usa `idUsuario` en lugar de `idFactura`
- ✅ Repository actualizado con 4 métodos nuevos para Usuario
- ✅ Permite historial completo de conversaciones por cliente
- ⚠️ Requiere script de migración SQL (pendiente)

**Documentación completa:** `docs/sprints/SPRINT_3/decisiones/DECISION_CHATS_LIGADOS_USUARIO.md`

---

