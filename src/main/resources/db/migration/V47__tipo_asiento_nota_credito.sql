-- T-047 (review 1, criterio 33): tipo propio para los asientos de las notas de crédito.
--
-- asientos_contables.tipo es un ENUM de MySQL en producción (dump facturas_monrachem_20261118_205706):
--   enum('AJUSTE','APERTURA','AUTOMATICO_COMPRA','AUTOMATICO_GASTO','AUTOMATICO_NOMINA','AUTOMATICO_PAGO',
--        'AUTOMATICO_VENTA','CIERRE','INGRESO','MANUAL') NOT NULL
-- Se amplía con AUTOMATICO_NOTA_CREDITO conservando los diez valores y el orden existentes (el nuevo va al final: MySQL
-- no reescribe las filas). Las filas existentes no se modifican: las reversiones de las notas 1 y 2 de producción siguen
-- siendo AUTOMATICO_VENTA con nota enlazada y asiento_revertido_id.
ALTER TABLE asientos_contables
    MODIFY COLUMN tipo ENUM('AJUSTE','APERTURA','AUTOMATICO_COMPRA','AUTOMATICO_GASTO','AUTOMATICO_NOMINA',
                            'AUTOMATICO_PAGO','AUTOMATICO_VENTA','CIERRE','INGRESO','MANUAL',
                            'AUTOMATICO_NOTA_CREDITO') NOT NULL;
