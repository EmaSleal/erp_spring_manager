-- T-049 (review 1, criterio 19): tipo propio para el asiento de la devolución en dinero de un saldo a favor.
--
-- Una devolución es una salida de dinero, no un cobro: con AUTOMATICO_PAGO cualquier consulta o reporte por tipo la contaría
-- como pago recibido (mismo problema que T-047 resolvió con AUTOMATICO_NOTA_CREDITO, V47).
-- asientos_contables.tipo es un ENUM de MySQL; tras V47 tiene once valores. Se amplía con AUTOMATICO_DEVOLUCION_SALDO_FAVOR
-- conservando los once valores y su orden (el nuevo va al final: MySQL no reescribe las filas). No se modifica ninguna fila.
ALTER TABLE asientos_contables
    MODIFY COLUMN tipo ENUM('AJUSTE','APERTURA','AUTOMATICO_COMPRA','AUTOMATICO_GASTO','AUTOMATICO_NOMINA',
                            'AUTOMATICO_PAGO','AUTOMATICO_VENTA','CIERRE','INGRESO','MANUAL',
                            'AUTOMATICO_NOTA_CREDITO','AUTOMATICO_DEVOLUCION_SALDO_FAVOR') NOT NULL;
