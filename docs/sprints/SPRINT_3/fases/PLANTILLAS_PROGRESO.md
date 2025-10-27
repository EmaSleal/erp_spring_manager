# 📝 PROGRESO CREACIÓN PLANTILLAS WHATSAPP

**Fecha inicio:** 20 octubre 2025  
**Estado:** 🔄 EN PROGRESO  

---

## 📊 RESUMEN GENERAL

```
Total plantillas: 5
Creadas: 0/5 (0%)
En aprobación: 0/5
Aprobadas: 0/5
```

---

## PLANTILLA 1: `factura_generada` ⏸️

**Estado:** ⏸️ EN PROGRESO  
**Prioridad:** CRÍTICA  
**Tiempo estimado:** 30 minutos

### Checklist de Creación

#### Configuración Básica
- [ ] Acceder a Message Templates
- [ ] Click en "Create Template"
- [ ] Category: UTILITY ✅
- [ ] Name: `factura_generada` ✅
- [ ] Language: Spanish (Mexico) - es_MX ✅

#### Header (Encabezado)
- [ ] Agregar header
- [ ] Tipo: TEXT
- [ ] Contenido: `📄 Nueva Factura Generada`

#### Body (Cuerpo) - OBLIGATORIO
- [ ] Copiar body desde documentación
- [ ] Verificar 5 parámetros: {{1}} a {{5}}
- [ ] Preview: verificar formato

**Body a copiar:**
```
Hola {{1}}, tu factura #{{2}} por {{3}} ha sido generada exitosamente.

📅 Fecha de vencimiento: {{4}}
🔗 Ver detalles: {{5}}

Gracias por tu preferencia.
```

#### Footer (Pie)
- [ ] Agregar footer
- [ ] Contenido: `Astro Desarrollo - Sistema ERP`

#### Buttons (Opcional)
- [ ] Agregar botón "Ver Factura"
- [ ] Tipo: URL dinámico

#### Envío
- [ ] Revisar preview completo
- [ ] Click en "Submit"
- [ ] Copiar Template ID: `_______________`
- [ ] Anotar fecha envío: `_______________`

### Template ID
```
Template ID: [PENDIENTE]
Estado: [PENDIENTE]
Fecha envío: [PENDIENTE]
```

---

## PLANTILLA 2: `recordatorio_pago` ⏸️

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** ALTA

### Checklist de Creación

#### Configuración Básica
- [ ] Click en "Create Template"
- [ ] Category: UTILITY
- [ ] Name: `recordatorio_pago`
- [ ] Language: Spanish (Mexico) - es_MX

#### Header
- [ ] Tipo: TEXT
- [ ] Contenido: `🔔 Recordatorio de Pago`

#### Body
**Body a copiar:**
```
Hola {{1}},

Te recordamos amablemente que tu factura #{{2}} por {{3}} vence el {{4}}.

💳 Puedes realizar el pago en:
{{5}}

¿Tienes alguna pregunta? Responde a este mensaje y con gusto te atendemos.

Gracias por tu puntualidad.
```

**Parámetros:**
- {{1}} = Nombre del cliente
- {{2}} = Número de factura
- {{3}} = Monto
- {{4}} = Fecha vencimiento
- {{5}} = Métodos de pago

#### Footer
- [ ] Contenido: `Este es un recordatorio amistoso`

#### Envío
- [ ] Revisar preview
- [ ] Submit
- [ ] Template ID: `_______________`

### Template ID
```
Template ID: [PENDIENTE]
Estado: [PENDIENTE]
```

---

## PLANTILLA 3: `pago_recibido` ⏸️

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** ALTA

### Checklist de Creación

#### Configuración Básica
- [ ] Click en "Create Template"
- [ ] Category: UTILITY
- [ ] Name: `pago_recibido`
- [ ] Language: Spanish (Mexico) - es_MX

#### Header
- [ ] Tipo: TEXT
- [ ] Contenido: `✅ ¡Pago Confirmado!`

#### Body
**Body a copiar:**
```
¡Excelente noticia, {{1}}!

Hemos recibido tu pago de {{2}} correspondiente a la factura #{{3}}.

📅 Fecha de pago: {{4}}
💳 Método de pago: {{5}}

Tu saldo está al corriente. Gracias por tu puntualidad.
```

**Parámetros:**
- {{1}} = Nombre del cliente
- {{2}} = Monto pagado
- {{3}} = Número de factura
- {{4}} = Fecha de pago
- {{5}} = Método de pago

#### Footer
- [ ] Contenido: `Gracias por tu confianza`

#### Envío
- [ ] Revisar preview
- [ ] Submit
- [ ] Template ID: `_______________`

### Template ID
```
Template ID: [PENDIENTE]
Estado: [PENDIENTE]
```

---

## PLANTILLA 4: `factura_vencida` ⏸️

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** MEDIA

### Checklist de Creación

#### Configuración Básica
- [ ] Click en "Create Template"
- [ ] Category: UTILITY
- [ ] Name: `factura_vencida`
- [ ] Language: Spanish (Mexico) - es_MX

#### Header
- [ ] Tipo: TEXT
- [ ] Contenido: `⚠️ Factura Vencida`

#### Body
**Body a copiar:**
```
Hola {{1}},

Te informamos que la factura #{{2}} por {{3}} venció el {{4}}.

Por favor, realiza el pago a la brevedad posible para evitar cargos adicionales o interrupción del servicio.

📞 ¿Necesitas ayuda o tienes alguna situación especial?
Contáctanos respondiendo este mensaje.

Estamos aquí para ayudarte.
```

**Parámetros:**
- {{1}} = Nombre del cliente
- {{2}} = Número de factura
- {{3}} = Monto
- {{4}} = Fecha vencimiento

#### Footer
- [ ] Contenido: `Gracias por tu atención`

#### Envío
- [ ] Revisar preview
- [ ] Submit
- [ ] Template ID: `_______________`

### Template ID
```
Template ID: [PENDIENTE]
Estado: [PENDIENTE]
```

---

## PLANTILLA 5: `bienvenida_cliente` ⏸️

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** BAJA

### Checklist de Creación

#### Configuración Básica
- [ ] Click en "Create Template"
- [ ] Category: UTILITY (o MARKETING)
- [ ] Name: `bienvenida_cliente`
- [ ] Language: Spanish (Mexico) - es_MX

#### Header
- [ ] Tipo: TEXT
- [ ] Contenido: `👋 ¡Bienvenido!`

#### Body
**Body a copiar:**
```
¡Hola {{1}}!

Bienvenido a Astro Desarrollo. Gracias por confiar en nosotros.

✨ A partir de ahora recibirás notificaciones importantes por WhatsApp:
• Facturas generadas
• Recordatorios de pago
• Confirmaciones de pago
• Atención personalizada

Responde "AYUDA" en cualquier momento para ver las opciones disponibles.

¡Estamos para servirte!
```

**Parámetros:**
- {{1}} = Nombre del cliente

#### Footer
- [ ] Contenido: `Astro Desarrollo - Tu aliado tecnológico`

#### Envío
- [ ] Revisar preview
- [ ] Submit
- [ ] Template ID: `_______________`

### Template ID
```
Template ID: [PENDIENTE]
Estado: [PENDIENTE]
```

---

## 📊 TABLA RESUMEN DE TEMPLATE IDs

| Plantilla | Template ID | Estado | Fecha Envío | Fecha Aprobación |
|-----------|-------------|--------|-------------|------------------|
| factura_generada | - | Pendiente | - | - |
| recordatorio_pago | - | Pendiente | - | - |
| pago_recibido | - | Pendiente | - | - |
| factura_vencida | - | Pendiente | - | - |
| bienvenida_cliente | - | Pendiente | - | - |

---

## 💡 CONSEJOS IMPORTANTES

### ✅ Hacer:
- Usar lenguaje claro y profesional
- Incluir emojis apropiados (moderadamente)
- Ser específico con los parámetros
- Categoría UTILITY para transacciones
- Revisar preview antes de enviar

### ❌ Evitar:
- Lenguaje promocional agresivo
- Palabras como "¡GRATIS!", "OFERTA!", "COMPRA YA!"
- Contenido engañoso
- URLs acortadas
- Más de 1024 caracteres en el body

---

## 🚨 TROUBLESHOOTING

### Si una plantilla es rechazada:
1. Lee el mensaje de rechazo en la notificación
2. Identifica la causa (generalmente lenguaje promocional)
3. Edita el contenido
4. Cambia la categoría si es necesario
5. Reenvía a aprobación

### Tiempos de aprobación:
- **Normal:** 48-72 horas
- **Rápido:** 24-48 horas (poco común)
- **Lento:** 96+ horas (revisar estado)

---

**Última actualización:** 20 octubre 2025  
**Estado:** 🔄 EN PROGRESO  
**Progreso:** 0/5 plantillas creadas
