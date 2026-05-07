## 📊 CONTEXTO

### Diseño Original (26 octubre 2025)
```
MensajeWhatsApp -> idFactura -> Factura -> Cliente
```

Los mensajes estaban vinculados directamente a una factura específica, limitando la conversación a un único pedido.

### Problema Identificado
1. **Un usuario puede tener múltiples pedidos** - El diseño original forzaba crear múltiples hilos de conversación
2. **Comunicación fragmentada** - Cada factura tenía su propio chat aislado
3. **Experiencia de usuario deficiente** - Los clientes esperan un único chat continuo
4. **Falta de contexto** - No se podía discutir múltiples pedidos en una conversación

---

