## 📝 Notas Importantes

### Integración Contable
- Al **confirmar** un pago, se genera automáticamente un asiento contable:
  - **DEBE**: Cuenta de caja/banco según método de pago
  - **HABER**: Cuentas por cobrar (1.1.03)

### Métodos de Pago (Hacienda CR)
- Códigos oficiales según Anexo 4.4
- Validación en CHECK constraint
- Enum Java sincronizado con BD

### Estados del Pago
- **PENDIENTE**: Registrado pero no confirmado
- **CONFIRMADO**: Genera asiento contable
- **RECHAZADO**: Validación fallida
- **ANULADO**: Revierte asiento contable
- **CONCILIADO**: Validado contra extracto bancario

### Seguridad
- Permisos críticos: ELIMINAR, CONFIRMAR, ANULAR
- Solo ADMIN puede anular pagos confirmados
- CONTADOR puede conciliar
- VENDEDOR solo ver y crear

---

