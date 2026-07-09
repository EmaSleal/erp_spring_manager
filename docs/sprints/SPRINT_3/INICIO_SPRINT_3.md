# 🚀 INICIO SPRINT 3 - GUÍA DE ACCIÓN INMEDIATA

**Fecha de inicio:** 21 de octubre de 2025  
**Acción requerida:** INICIAR HOY proceso de aprobación Meta WhatsApp  
**Criticidad:** ⚡ ALTA (el proceso toma 3-7 días)

---

## ⚠️ ACCIÓN CRÍTICA - HACER HOY (21 octubre)

### Paso 1: Crear Cuenta Facebook Business (30 min)

1. **Ir a:** https://business.facebook.com
2. **Crear cuenta** con email corporativo
3. **Verificar:**
   - Email
   - Número de teléfono
   - Identidad

**Datos necesarios:**
- Nombre de empresa: [A DEFINIR]
- Email corporativo: [A DEFINIR]
- Teléfono corporativo: [A DEFINIR]
- Dirección física: [A DEFINIR]

### Paso 2: Crear App en Meta for Developers (30 min)

1. **Ir a:** https://developers.facebook.com
2. **Mis Apps** → **Crear app**
3. **Seleccionar:** "Empresa"
4. **Configurar:**
   ```
   Nombre de la app: WhatsApp Orders Manager
   Email de contacto: [EMAIL CORPORATIVO]
   Cuenta de empresa: [LA CREADA EN PASO 1]
   ```

### Paso 3: Agregar WhatsApp a la App (20 min)

1. **Dashboard de la app** → **Agregar producto**
2. **Seleccionar:** "WhatsApp"
3. **Configurar:**
   - Aceptar términos
   - Configurar cuenta de WhatsApp Business

### Paso 4: Verificar Número de Teléfono (Inmediato)

1. **Ir a:** WhatsApp → Configuración → Números de teléfono
2. **Agregar número:**
   - **Opción A:** Nuevo número dedicado (RECOMENDADO)
   - **Opción B:** Migrar número existente
3. **Verificar vía SMS**
4. **Guardar:**
   - ✅ Phone Number ID: `______________`

### Paso 5: Obtener Access Token (10 min)

1. **Ir a:** WhatsApp → Configuración → API Setup
2. **Generar token temporal** (24h)
3. **Copiar y guardar:**
   - ✅ Access Token temporal: `______________`

**NOTA:** Generaremos token permanente después de aprobar plantillas

---

## 📝 PASO 6: CREAR PLANTILLAS (1 hora)

### Plantilla 1: `factura_generada`

**En Meta for Developers:**
1. WhatsApp → Message Templates → Create Template
2. **Configurar:**
   ```
   Nombre: factura_generada
   Categoría: UTILITY
   Idioma: Spanish (Mexico)
   ```

3. **Header (opcional):**
   ```
   Tipo: TEXT
   Contenido: 📄 Nueva Factura
   ```

4. **Body:**
   ```
   Hola {{1}}, tu factura #{{2}} por {{3}} ha sido generada.
   
   📅 Fecha de vencimiento: {{4}}
   🔗 Ver detalles: {{5}}
   
   Gracias por tu preferencia.
   ```

5. **Footer (opcional):**
   ```
   WhatsApp Orders Manager
   ```

6. **Buttons (opcional):**
   - Ver Factura (URL dinámica)

7. **Enviar a aprobación**

### Plantilla 2: `recordatorio_pago`

```
Nombre: recordatorio_pago
Categoría: UTILITY
Idioma: Spanish (Mexico)

Body:
Hola {{1}}, 

🔔 Recordatorio amistoso:
Tu factura #{{2}} por {{3}} vence {{4}}.

💳 Puedes realizar el pago en:
{{5}}

¿Tienes alguna pregunta? Responde a este mensaje.

Gracias por tu preferencia.
```

### Plantilla 3: `pago_recibido`

```
Nombre: pago_recibido
Categoría: UTILITY
Idioma: Spanish (Mexico)

Body:
✅ ¡Pago confirmado!

Hola {{1}}, hemos recibido tu pago de {{2}} por la factura #{{3}}.

Fecha de pago: {{4}}
Método: {{5}}

Gracias por tu puntualidad.
```

### Plantilla 4: `factura_vencida`

```
Nombre: factura_vencida
Categoría: UTILITY
Idioma: Spanish (Mexico)

Body:
⚠️ Factura vencida

Hola {{1}},

La factura #{{2}} por {{3}} venció el {{4}}.

Por favor, realiza el pago a la brevedad para evitar cargos adicionales.

📞 ¿Necesitas ayuda? Contáctanos respondiendo este mensaje.
```

### Plantilla 5: `bienvenida_cliente`

```
Nombre: bienvenida_cliente
Categoría: MARKETING
Idioma: Spanish (Mexico)

Body:
¡Bienvenido/a {{1}}! 👋

Gracias por confiar en nosotros. A partir de ahora recibirás notificaciones de tus facturas y pedidos por WhatsApp.

✨ Beneficios:
• Alertas de facturas
• Recordatorios de pago
• Atención personalizada

Responde "AYUDA" para ver opciones disponibles.
```

---

## ⏱️ TIMELINE DE APROBACIÓN

```
Día 1 (HOY - 21 oct)  → Crear cuenta + app + plantillas      [3h]
Día 2 (22 oct)        → Verificaciones adicionales           [1h]
Día 3-5 (23-25 oct)   → ESPERAR aprobación Meta             [0h]
Día 6 (26 oct)        → Generar token permanente            [0.5h]
Día 7 (27 oct)        → Configurar webhook                  [1h]
```

**Checkpoint crítico:** Día 7 (27 oct) → Cuenta aprobada ✅

---

## 🔧 CONFIGURACIÓN TÉCNICA (Día 6-7)

### Generar Access Token Permanente

1. **Ir a:** Graph API Explorer
2. **Seleccionar app:** WhatsApp Orders Manager
3. **Permisos necesarios:**
   - `whatsapp_business_messaging`
   - `whatsapp_business_management`
4. **Generar token**
5. **Guardar en variables de entorno:**

```env
# .env (NO COMMITEAR)
META_WHATSAPP_PHONE_NUMBER_ID=123456789012345
META_WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxxxxxxx
META_WEBHOOK_VERIFY_TOKEN=mi_token_secreto_12345
```

### Configurar Webhook

1. **Preparar URL pública:**
   - **Opción A:** ngrok (desarrollo)
     ```bash
     ngrok http 8080
     # URL: https://abc123.ngrok.io
     ```
   - **Opción B:** Servidor staging
     ```
     https://staging.tudominio.com
     ```

2. **En Meta for Developers:**
   - WhatsApp → Configuration → Webhook
   - **Callback URL:** `https://TU-URL/api/whatsapp/webhook`
   - **Verify Token:** `mi_token_secreto_12345`
   - **Webhook fields:** ☑️ messages, ☑️ message_status

3. **Probar webhook:**
   - Meta enviará GET con challenge
   - Nuestro endpoint debe responder con el challenge

---

## 📋 CHECKLIST DÍA 1 (HOY)

### Tareas Administrativas
- [ ] ✅ Crear cuenta Facebook Business
- [ ] ✅ Verificar email
- [ ] ✅ Verificar teléfono
- [ ] ✅ Agregar información del negocio

### Tareas Técnicas
- [ ] ✅ Crear app en Meta for Developers
- [ ] ✅ Agregar producto WhatsApp
- [ ] ✅ Agregar y verificar número de teléfono
- [ ] ✅ Copiar Phone Number ID
- [ ] ✅ Generar Access Token temporal

### Tareas de Contenido
- [ ] ✅ Crear plantilla `factura_generada`
- [ ] ✅ Crear plantilla `recordatorio_pago`
- [ ] ✅ Crear plantilla `pago_recibido`
- [ ] ✅ Crear plantilla `factura_vencida`
- [ ] ✅ Crear plantilla `bienvenida_cliente`
- [ ] ✅ Enviar todas a aprobación

### Documentación
- [ ] ✅ Guardar credenciales en lugar seguro
- [ ] ✅ Documentar Phone Number ID
- [ ] ✅ Documentar Access Token temporal
- [ ] ✅ Anotar fecha de envío de plantillas

---

## 📞 CONTACTOS Y RECURSOS

### Documentación Oficial Meta
- **Inicio rápido:** https://developers.facebook.com/docs/whatsapp/cloud-api/get-started
- **Plantillas:** https://developers.facebook.com/docs/whatsapp/message-templates
- **Webhooks:** https://developers.facebook.com/docs/whatsapp/webhooks
- **API Reference:** https://developers.facebook.com/docs/whatsapp/cloud-api/reference

### Herramientas
- **Graph API Explorer:** https://developers.facebook.com/tools/explorer
- **Business Manager:** https://business.facebook.com
- **WhatsApp Manager:** https://business.facebook.com/wa/manage

### Soporte
- **Centro de ayuda:** https://developers.facebook.com/support
- **Foro de desarrolladores:** https://developers.facebook.com/community

---

## ⚠️ PROBLEMAS COMUNES Y SOLUCIONES

### Problema: "Plantilla rechazada"
**Solución:**
- No usar lenguaje promocional agresivo
- Evitar palabras como "¡GRATIS!", "OFERTA!", "COMPRA YA!"
- Usar categoría UTILITY para transaccionales
- Usar MARKETING solo para promociones

### Problema: "Cuenta en revisión por más de 7 días"
**Solución:**
- Verificar que toda la información esté completa
- Subir documentación del negocio si solicitan
- Contactar soporte de Meta

### Problema: "Access Token expiró"
**Solución:**
- Los tokens temporales duran 24h
- Generar token permanente después de aprobación
- Configurar renovación automática

---

## 🎯 OBJETIVOS DEL DÍA 1

**Al final del día DEBES tener:**
1. ✅ Cuenta Facebook Business creada y verificada
2. ✅ App de WhatsApp creada
3. ✅ Número de teléfono verificado
4. ✅ Phone Number ID documentado
5. ✅ 5 plantillas enviadas a aprobación
6. ✅ Access Token temporal guardado

**Tiempo estimado:** 3-4 horas

---

## 📅 PRÓXIMOS PASOS (Día 2-7)

### Día 2 (22 octubre)
- Verificar estado de plantillas
- Completar perfil de empresa
- Subir logo si solicitan

### Día 3-5 (23-25 octubre)
- **ESPERAR aprobación**
- Mientras tanto: Desarrollar entidades de BD
- Mientras tanto: Desarrollar DTOs
- Mientras tanto: Preparar estructura de servicios

### Día 6 (26 octubre)
- Si aprobado: Generar Access Token permanente
- Configurar variables de entorno
- Preparar ngrok o staging

### Día 7 (27 octubre)
- Configurar webhook
- Realizar pruebas de envío
- ✅ **CHECKPOINT:** Todo listo para empezar desarrollo

---

## 🚨 PLAN DE CONTINGENCIA

**Si aprobación tarda más de 7 días:**

1. **Días 1-7:** Desarrollo completo de backend sin enviar mensajes reales
2. **Día 8:** Contactar soporte de Meta
3. **Día 9:** Evaluar usar Twilio temporalmente
4. **Día 10:** Decisión final: esperar o cambiar a Twilio

**Costo de cambiar a Twilio:**
- Implementación: 4 horas adicionales
- Costo mensual: +$5-10 USD

---

## 📊 MÉTRICAS DE ÉXITO - DÍA 1

- ⏱️ **Tiempo invertido:** 3-4 horas
- ✅ **Cuenta creada:** Sí/No
- ✅ **Plantillas enviadas:** 5/5
- ✅ **Credenciales guardadas:** Sí/No
- 🎯 **Listo para aprobación:** Sí/No

---

**Preparado por:** GitHub Copilot  
**Fecha:** 20 de octubre de 2025  
**Próxima revisión:** 22 de octubre de 2025  

**¡ÉXITO EN EL SPRINT 3!** 🚀
