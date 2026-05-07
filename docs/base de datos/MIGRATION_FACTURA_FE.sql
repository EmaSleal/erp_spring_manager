-- ================================================
-- MIGRACIÓN: Agregar campos de FE a tabla factura
-- Facturación Electrónica Costa Rica v4.4 - Fase 7
-- Fecha: 2026-02-22
-- ================================================

-- 1. Agregar columnas a la tabla factura
ALTER TABLE factura 
ADD COLUMN condicion_venta_fe VARCHAR(50) NULL COMMENT 'Condición de venta: CONTADO, CREDITO, CONSIGNACION, etc.',
ADD COLUMN medio_pago_fe VARCHAR(50) NULL COMMENT 'Medio de pago: EFECTIVO, TARJETA, CHEQUE, TRANSFERENCIA_DEPOSITO, etc.',
ADD COLUMN moneda_fe VARCHAR(3) NULL COMMENT 'Moneda: CRC, USD, EUR',
ADD COLUMN tipo_cambio DECIMAL(18,5) NULL COMMENT 'Tipo de cambio (obligatorio si moneda != CRC)',
ADD COLUMN plazo_credito INT NULL COMMENT 'Plazo de crédito en días (obligatorio si condición = CREDITO)';

-- 2. Establecer valores por defecto para facturas existentes
UPDATE factura 
SET 
    condicion_venta_fe = 'CONTADO',
    medio_pago_fe = 'EFECTIVO',
    moneda_fe = 'CRC',
    tipo_cambio = 1.00000
WHERE condicion_venta_fe IS NULL;

-- 3. Índices para mejorar rendimiento en consultas
CREATE INDEX idx_factura_condicion_venta ON factura(condicion_venta_fe);
CREATE INDEX idx_factura_medio_pago ON factura(medio_pago_fe);
CREATE INDEX idx_factura_moneda ON factura(moneda_fe);

-- 4. Verificar resultados
SELECT 
    'Facturas migradas' AS tipo,
    COUNT(*) AS cantidad,
    COUNT(DISTINCT condicion_venta_fe) AS condiciones_diferentes,
    COUNT(DISTINCT medio_pago_fe) AS medios_pago_diferentes,
    COUNT(DISTINCT moneda_fe) AS monedas_diferentes
FROM factura;

-- 5. Ver ejemplos de facturas actualizadas
SELECT 
    idFactura,
    numeroFactura,
    condicion_venta_fe,
    medio_pago_fe,
    moneda_fe,
    tipo_cambio,
    plazo_credito,
    total
FROM factura
ORDER BY idFactura DESC
LIMIT 10;

-- ================================================
-- NOTAS IMPORTANTES:
-- ================================================
-- 1. CONDICIÓN DE VENTA (condicion_venta_fe):
--    - CONTADO: Pago al momento de la compra
--    - CREDITO: Pago diferido (requiere plazo_credito)
--    - CONSIGNACION: Mercadería entregada sin pago inmediato
--    - APARTADO: Producto reservado con anticipo
--    - Otros según catálogo de Hacienda
--
-- 2. MEDIO DE PAGO (medio_pago_fe):
--    - EFECTIVO: Dinero en efectivo
--    - TARJETA: Tarjeta de crédito/débito
--    - CHEQUE: Cheque bancario
--    - TRANSFERENCIA_DEPOSITO: Transferencia o depósito bancario
--    - RECAUDADO_TERCEROS: Cobrado por un tercero
--    - OTROS: Otros medios de pago
--
-- 3. MONEDA (moneda_fe):
--    - CRC: Colón costarricense (por defecto)
--    - USD: Dólar estadounidense
--    - EUR: Euro
--    - Si moneda != CRC, el campo tipo_cambio es OBLIGATORIO
--
-- 4. TIPO DE CAMBIO (tipo_cambio):
--    - DECIMAL(18,5) para alta precisión
--    - Ejemplo: 532.15000 (1 USD = 532.15 CRC)
--    - Obligatorio cuando moneda_fe != 'CRC'
--    - Para CRC siempre debe ser 1.00000
--
-- 5. PLAZO DE CRÉDITO (plazo_credito):
--    - Días de crédito otorgado
--    - Obligatorio cuando condicion_venta_fe = 'CREDITO'
--    - Ejemplo: 30, 60, 90 días
--
-- 6. VALORES POR DEFECTO:
--    - Las facturas existentes se marcan como CONTADO + EFECTIVO + CRC
--    - Esto asegura compatibilidad con facturas antiguas
--    - Nuevas facturas deben especificar estos valores
-- ================================================

-- ================================================
-- VALIDACIONES RECOMENDADAS A NIVEL APLICACIÓN:
-- ================================================
-- 1. Si condicion_venta_fe = 'CREDITO' → plazo_credito debe ser > 0
-- 2. Si moneda_fe != 'CRC' → tipo_cambio debe ser > 0
-- 3. Si moneda_fe = 'CRC' → tipo_cambio = 1.00000
-- 4. Al crear comprobante electrónico, validar que estos campos no sean NULL
-- ================================================
