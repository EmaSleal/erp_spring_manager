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

