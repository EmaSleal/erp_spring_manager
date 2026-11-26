-- ============================================================================
-- V62: régimen del proveedor y de la CxP, y aceptación NO_APLICA (T-058, ADR-074 §2-3, §5-6)
-- ============================================================================
-- 1. proveedores.regimen_hacienda: régimen que Hacienda informa del proveedor en /fe/ae (GENERAL, SIMPLIFICADO,
--    NO_CONTRIBUYENTE, OTRO o SIN_VERIFICAR). NOT NULL con SIN_VERIFICAR por defecto: los proveedores existentes quedan sin
--    verificar hasta que se consulte (al crearlos, al cambiarles la cédula o con «Verificar régimen»).
--    proveedores.regimen_verificado_en: cuándo se obtuvo, con el reloj de la aplicación; NULL mientras esté SIN_VERIFICAR.
--
-- 2. cuentas_por_pagar.regimen_proveedor: copia del régimen del proveedor al registrar la CxP (mismo dominio, NOT NULL).
--    Las CxP existentes quedan SIN_VERIFICAR: no se consultó a Hacienda cuando se registraron y no se inventa un régimen.
--
-- 3. Aceptación NO_APLICA: cuentas_por_pagar.estado_aceptacion ya es VARCHAR(12) sin restricción CHECK (V55), y 'NO_APLICA'
--    (9 caracteres) cabe: no hace falta alterar la columna. Las CxP existentes conservan su estado: ninguna se reasigna.
--
-- 4. Tarifa del bien e IVA trasladado (ADR-074 §5-6): cuenta_pagar_linea ya los guarda por separado (V55): codigo_tarifa
--    es la tarifa del bien (CodigoTarifaIVA) e iva es el IVA trasladado en el comprobante. No se agrega columna ni se
--    rellenan las líneas existentes.
-- ============================================================================

ALTER TABLE proveedores
    ADD COLUMN regimen_hacienda     VARCHAR(20) NOT NULL DEFAULT 'SIN_VERIFICAR',
    ADD COLUMN regimen_verificado_en DATETIME   NULL,
    ADD CONSTRAINT ck_proveedores_regimen_hacienda
        CHECK (regimen_hacienda IN ('GENERAL', 'SIMPLIFICADO', 'NO_CONTRIBUYENTE', 'OTRO', 'SIN_VERIFICAR'));

ALTER TABLE cuentas_por_pagar
    ADD COLUMN regimen_proveedor VARCHAR(20) NOT NULL DEFAULT 'SIN_VERIFICAR',
    ADD CONSTRAINT ck_cpp_regimen_proveedor
        CHECK (regimen_proveedor IN ('GENERAL', 'SIMPLIFICADO', 'NO_CONTRIBUYENTE', 'OTRO', 'SIN_VERIFICAR'));
