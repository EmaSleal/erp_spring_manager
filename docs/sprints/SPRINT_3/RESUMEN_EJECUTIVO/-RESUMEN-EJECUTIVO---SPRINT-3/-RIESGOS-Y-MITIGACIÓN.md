## ⚠️ RIESGOS Y MITIGACIÓN

### Riesgo 1: Aprobación Meta demora > 7 días
**Probabilidad:** Media (30%)  
**Impacto:** Alto (bloquea sprint)  
**Mitigación:**
- Iniciar solicitud HOY
- Desarrollar BD/DTOs mientras esperamos
- Plan B: Cambiar a Twilio (4h adicionales)

### Riesgo 2: Plantillas rechazadas
**Probabilidad:** Baja (15%)  
**Impacto:** Medio (rediseñar plantillas)  
**Mitigación:**
- Usar categoría UTILITY (no MARKETING)
- Evitar lenguaje promocional
- Seguir guidelines de Meta

### Riesgo 3: APIs de tipo cambio caídas
**Probabilidad:** Baja (10%)  
**Impacto:** Medio (entrada manual temporal)  
**Mitigación:**
- Sistema híbrido (API + manual)
- 2 APIs diferentes (ExchangeRate + Banxico)
- Caché de últimos valores

---

