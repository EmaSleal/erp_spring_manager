## 🎓 Cambios en Flujos de Negocio

### Flujo 1: Registrar Pago Normal

1. Usuario selecciona **cliente** (nuevo campo obligatorio)
2. Usuario selecciona **factura** del cliente
3. Sistema carga `saldoPendiente` de la factura
4. Usuario ingresa **monto** ≤ saldoPendiente
5. Usuario selecciona **tipo de pago**: TOTAL o PARCIAL
6. Usuario selecciona **método de pago**
7. Si método requiere referencia → solicitar referencia
8. Sistema **genera número** automático: `PAG-20260119-0001`
9. Sistema guarda pago con estado `CONFIRMADO`
10. Sistema actualiza saldo de factura

---

### Flujo 2: Registrar Adelanto (NUEVO)

1. Usuario selecciona **cliente**
2. Usuario selecciona **tipo de pago**: ADELANTO
3. Sistema **oculta** campo factura (no requerido)
4. Usuario ingresa **monto** del adelanto
5. Usuario selecciona **método de pago**
6. Sistema **genera número**: `PAG-20260119-0002`
7. Sistema guarda pago:
   - `cliente` = Cliente seleccionado
   - `factura` = NULL
   - `tipoPago` = ADELANTO
   - `estado` = CONFIRMADO
8. Adelanto queda disponible para aplicar a futuras facturas

---

### Flujo 3: Aplicar Adelanto a Factura (FUTURO)

1. Usuario crea/edita factura de cliente
2. Sistema muestra adelantos disponibles del cliente
3. Usuario selecciona adelanto(s) a aplicar
4. Sistema actualiza pago(s):
   - `factura` = Factura actual
   - `tipoPago` = PARCIAL (o TOTAL si cubre todo)
5. Sistema recalcula saldo de factura

---

### Flujo 4: Anular Pago

1. Usuario abre detalle de pago
2. Si pago es editable → mostrar botón "Anular"
3. Usuario hace clic en "Anular"
4. Sistema solicita **motivo de anulación**
5. Usuario ingresa motivo
6. Sistema ejecuta `pago.anular(motivo, usuarioId)`:
   - `estado` = ANULADO
   - `anuladoPor` = ID del usuario actual
   - `anuladoEn` = Fecha/hora actual
   - `motivoAnulacion` = Motivo ingresado
7. Sistema recalcula saldo de factura
8. Sistema registra en log de auditoría

---

